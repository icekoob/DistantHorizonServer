package com.dibujaron.distanthorizon.database.impl

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.database.persistence.AccountInfo
import com.dibujaron.distanthorizon.database.persistence.ActorInfo
import com.dibujaron.distanthorizon.database.persistence.PersistenceDatabase
import com.dibujaron.distanthorizon.database.persistence.ShipInfo
import com.dibujaron.distanthorizon.orbiter.CommodityType
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipClassManager
import com.dibujaron.distanthorizon.ship.ShipColor
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Color

class ExPersistenceDatabase : PersistenceDatabase {
    override fun selectOrCreateAccount(accountName: String): AccountInfo {
        val nameFilter = (ExDatabase.Account.accountName eq accountName)
        val accountRow = transaction {
            //todo better way to do "create if not exists"
            //todo too many queries, figure out how to do joining properly with this framework
            val firstResult: ResultRow? = ExDatabase.Account
                .select { nameFilter }
                .firstOrNull()

            if (firstResult == null) {
                val id = ExDatabase.Account.insertAndGetId {
                    it[ExDatabase.Account.accountName] = accountName
                }
                val idFilter = (ExDatabase.Account.id eq id)
                ExDatabase.Account
                    .select { idFilter }
                    .first()
            } else {
                firstResult
            }
        }
        val accountIdFilter = (ExDatabase.Actor.ownedByAccount eq accountRow[ExDatabase.Account.id])
        val actors = transaction {
            ExDatabase.Actor.join(
                ExDatabase.Ship,
                JoinType.INNER,
                additionalConstraint = { ExDatabase.Actor.currentShip eq ExDatabase.Ship.id })
                .select(accountIdFilter)
                .map { mapActorInfo(it) }
                .toList()
        }
        return AccountInfoInternal(
            accountRow[ExDatabase.Account.id],
            accountRow[ExDatabase.Account.accountName],
            actors
        )
    }

    inner class AccountInfoInternal(
        val id: EntityID<Int>,
        accountName: String,
        actors: List<ActorInfoInternal>
    ) : AccountInfo(accountName, actors)

    inner class ActorInfoInternal(
        val id: EntityID<Int>,
        displayName: String,
        balance: Int,
        lastDockedStation: Station?,
        ship: ShipInfoInternal
    ) : ActorInfo(id.value, displayName, balance, lastDockedStation, ship)

    inner class ShipInfoInternal(
        val id: EntityID<Int>,
        shipClass: ShipClass,
        primaryColor: ShipColor,
        secondaryColor: ShipColor,
        holdMap: MutableMap<CommodityType, Int>
    ) : ShipInfo(shipClass, primaryColor, secondaryColor, holdMap)

    private fun mapActorInfo(row: ResultRow): ActorInfoInternal {
        return ActorInfoInternal(
            row[ExDatabase.Actor.id],
            row[ExDatabase.Actor.displayName],
            row[ExDatabase.Actor.balance],
            row[ExDatabase.Actor.lastDockedStation]?.let { OrbiterManager.getStationRequired(it) },
            mapShipInfo(row)
        )
    }

    private fun mapShipInfo(row: ResultRow): ShipInfoInternal {
        val holdMap = HashMap<CommodityType, Int>()
        CommodityType.values().forEach { ct ->
            val commodityName = ct.identifyingName
            val col: Column<Int> = ExDatabase.Ship.columns.find { col -> col.name == commodityName} as Column<Int>
            holdMap[ct] = row[col]
        }
        return ShipInfoInternal(
            row[ExDatabase.Ship.id],
            ShipClassManager.getShipClassRequired(row[ExDatabase.Ship.shipClass]),
            ShipColor.fromInt(row[ExDatabase.Ship.primaryColor]),
            ShipColor.fromInt(row[ExDatabase.Ship.secondaryColor]),
            holdMap
        )
    }

    override fun createNewActorForAccount(accountInfo: AccountInfo, actorDisplayName: String): AccountInfo? {
        if (accountInfo is AccountInfoInternal) {
            val acctId = accountInfo.id
            transaction {
                val shipId = ExDatabase.Ship.insertAndGetId {
                    it[shipClass] = DHServer.playerStartingShip
                    it[primaryColor] = ShipColor(Color(128, 128, 128)).toInt()//ShipColor(Color(0,148,255)),
                    it[secondaryColor] = ShipColor(Color(205, 106, 0)).toInt()
                }

                ExDatabase.Actor.insert {
                    it[ownedByAccount] = acctId
                    it[displayName] = actorDisplayName
                    it[balance] = DHServer.DEFAULT_BALANCE
                    it[lastDockedStation] = null
                    it[currentShip] = shipId
                }
            }
            return selectOrCreateAccount(accountInfo.accountName)
        } else {
            throw IllegalStateException("Object must be from same database")
        }
    }

    override fun deleteActor(actorInfo: ActorInfo) {
        if (actorInfo is ActorInfoInternal) {
            val ship = actorInfo.ship
            if (ship is ShipInfoInternal) {
                val shipIdFilter = (ExDatabase.Ship.id eq ship.id)
                val actorIdFilter = (ExDatabase.Actor.id eq actorInfo.id)
                val routeActorIdFilter = (ExDatabase.Route.plottedBy eq actorInfo.id)
                transaction {
                    ExDatabase.Route.update({routeActorIdFilter}){
                        it[plottedBy] = null
                    }
                    ExDatabase.Actor.deleteWhere { actorIdFilter }
                    ExDatabase.Ship.deleteWhere { shipIdFilter }
                }
            }
        }
        throw IllegalStateException("Object must be from same database")
    }

    override fun updateShipOfActor(actor: ActorInfo, ship: ShipInfo): ActorInfo? {
        if (actor is ActorInfoInternal) {
            val oldShip = actor.ship
            if (oldShip is ShipInfoInternal) {
                val shipIdFilter = (ExDatabase.Ship.id eq oldShip.id)
                transaction {
                    ExDatabase.Ship.update({ shipIdFilter }) {
                        it[shipClass] = ship.shipClass.qualifiedName
                        it[primaryColor] = ship.primaryColor.toInt()
                        it[secondaryColor] = ship.secondaryColor.toInt()
                    }
                }
                return transaction {
                    ExDatabase.Actor.join(
                        ExDatabase.Ship,
                        JoinType.INNER,
                        additionalConstraint = { ExDatabase.Actor.currentShip eq ExDatabase.Ship.id })
                        .select { ExDatabase.Actor.id eq actor.id }
                        .map { mapActorInfo(it) }
                        .first()
                }
            }
        }
        throw java.lang.IllegalStateException("Object must be from same db")
    }

    override fun updateActorBalance(actor: ActorInfo, newBal: Int): ActorInfo? {
        if (actor is ActorInfoInternal) {
            val actorIdFilter = (ExDatabase.Actor.id eq actor.id)
            transaction {
                ExDatabase.Actor.update({ actorIdFilter }) {
                    it[balance] = newBal
                }
            }
            return transaction {
                ExDatabase.Actor.join(
                    ExDatabase.Ship,
                    JoinType.INNER,
                    additionalConstraint = { ExDatabase.Actor.currentShip eq ExDatabase.Ship.id })
                    .select { ExDatabase.Actor.id eq actor.id }
                    .map { mapActorInfo(it) }
                    .first()
            }
        }
        throw java.lang.IllegalStateException("Object must be from same db")
    }

    override fun updateActorLastDockedStation(actor: ActorInfo, station: Station): ActorInfo? {
        if (actor is ActorInfoInternal) {
            val actorIdFilter = (ExDatabase.Actor.id eq actor.id)
            transaction {
                ExDatabase.Actor.update({ actorIdFilter }) {
                    it[lastDockedStation] = station.name
                }
            }
            return transaction {
                ExDatabase.Actor.join(
                    ExDatabase.Ship,
                    JoinType.INNER,
                    additionalConstraint = { ExDatabase.Actor.currentShip eq ExDatabase.Ship.id })
                    .select { ExDatabase.Actor.id eq actor.id }
                    .map { mapActorInfo(it) }
                    .first()
            }
        }
        throw java.lang.IllegalStateException("Object must be from same db")
    }

    override fun updateShipHold(ship: ShipInfo, commodity: CommodityType, amount: Int) {
        if(ship is ShipInfoInternal)
        {
            val shipIDFilter = (ExDatabase.Ship.id eq ship.id)
            val commodityCol: Column<Int> = ExDatabase.Ship.columns.find { col -> col.name == commodity.identifyingName} as Column<Int>
            transaction {
                ExDatabase.Ship.update ({ shipIDFilter }){
                    it[commodityCol] = amount
                }
            }
        } else {
            throw java.lang.IllegalStateException("Object must be from same db")
        }
    }
}