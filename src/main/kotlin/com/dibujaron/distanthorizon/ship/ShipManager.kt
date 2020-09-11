package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.player.PlayerManager
import java.util.*

object ShipManager {
    private val shipMap = HashMap<UUID, Ship>()
    private val shipsToAdd = LinkedList<Ship>()
    private val shipsToRemove = LinkedList<Ship>()

    init {
        /*OrbiterManager.getStations().asSequence()
            .filter{it.getStar().name.contains("Regalis")}
            .map { it.getState() }
            .map {
                Ship(
                    ShipClassManager.getShipClass("radi.kx6")!!,
                    ShipColor.random(),
                    ShipColor.random(),
                    it,
                    SimpleStraightLineController()
                )
            }
            .take(2).forEach{ shipsToAdd.add(it) }*/
    }

    fun getShips(): Collection<Ship> {
        return shipMap.values
    }

    fun getShipsInBucket(bucket: Int): Sequence<Ship> {
        if (bucket < 0 || bucket > DHServer.TICKS_PER_SECOND) {
            throw IllegalArgumentException("bucket must be between 0 and ticks per second")
        } else {
            val numBuckets = DHServer.TICKS_PER_SECOND
            return shipMap.entries.asSequence().filter { (it.key.hashCode() % numBuckets) == bucket }.map { it.value }
        }
    }

    fun tick() {
        if (!shipsToRemove.isEmpty()) {
            val shipsRemovedMessage = DHServer.composeMessageForShipsRemoved(shipsToRemove)
            PlayerManager.getPlayers().asSequence().forEach { it.sendShipsRemoved(shipsRemovedMessage) }
            shipsToRemove.forEach { shipMap.remove(it.uuid) }
            shipsToRemove.clear()
        }
        if(!shipsToAdd.isEmpty()) {
            shipsToAdd.forEach { shipMap[it.uuid] = it }
            val shipsAddedMessage = DHServer.composeMessageForShipsAdded(shipsToAdd)
            PlayerManager.getPlayers().asSequence().forEach { it.sendShipsAdded(shipsAddedMessage) }
            shipsToAdd.clear()
        }
        getShips().forEach { it.tick() }
    }

    fun markForAdd(ship: Ship) {
        shipsToAdd.add(ship)
    }

    fun markForRemove(ship: Ship) {
        shipsToRemove.add(ship)
    }
}