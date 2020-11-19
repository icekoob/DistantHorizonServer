package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.player.PlayerManager
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object ShipManager {
    private val shipMap = ConcurrentHashMap<UUID, Ship>()

    fun getShips(): Collection<Ship> {
        return shipMap.values
    }

    /*fun getShipsInBucket(bucket: Int): Sequence<Ship> {
        if (bucket < 0 || bucket > DHServer.TICKS_PER_SECOND) {
            throw IllegalArgumentException("bucket must be between 0 and ticks per second")
        } else {
            val numBuckets = DHServer.TICKS_PER_SECOND
            return shipMap.entries.asSequence().filter { (it.key.hashCode() % numBuckets) == bucket }.map { it.value }
        }
    }*/

    fun tick() {
        /*if (!shipsToRemove.isEmpty()) {
            val shipsRemovedMessage = DHServer.composeMessageForShipsRemoved(shipsToRemove)
            PlayerManager.getPlayers().asSequence().forEach { it.queueShipsRemovedMsg(shipsRemovedMessage) }
            shipsToRemove.forEach { shipMap.remove(it.uuid) }
            shipsToRemove.clear()
        }
        if(!shipsToAdd.isEmpty()) {
            shipsToAdd.forEach { shipMap[it.uuid] = it }
            val shipsAddedMessage = DHServer.composeMessageForShipsAdded(shipsToAdd)
            PlayerManager.getPlayers().asSequence().forEach { it.queueShipsAddedMsg(shipsAddedMessage) }
            shipsToAdd.clear()
        }*/
        getShips().forEach { it.tick() }
    }

    fun addShip(ship: Ship) {
        shipMap[ship.uuid] = ship
        val message = DHServer.composeMessageForShipsAdded(Collections.singletonList(ship))
        PlayerManager.getPlayers().asSequence().forEach { it.queueShipsAddedMsg(message) }
    }

    fun removeShip(ship: Ship) {
        val message = DHServer.composeMessageForShipsRemoved(Collections.singletonList(ship))
        PlayerManager.getPlayers().asSequence().forEach { it.queueShipsRemovedMsg(message) }
        shipMap.remove(ship.uuid)
    }
}