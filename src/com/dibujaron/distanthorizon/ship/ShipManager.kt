package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.ship.controller.AIShipController
import java.lang.IllegalArgumentException
import java.util.*

object ShipManager {
    private val shipMap = HashMap<UUID, Ship>()
    private val shipsToAdd = LinkedList<Ship>()
    private val shipsToRemove = LinkedList<Ship>()

    init {
        (0..5).asSequence()
            .map { OrbiterManager.getStations().random() }
            .map { it.getState() }
            .map { Ship(ShipClassManager.getShipClasses().random(), it, AIShipController()) }
            .forEach { shipsToAdd.add(it) }
    }

    fun getShips(): Collection<Ship> {
        return shipMap.values
    }

    fun getShipsInBucket(bucket: Int): Sequence<Ship> {
        if(bucket < 0 || bucket > DHServer.ticksPerSecond){
            throw IllegalArgumentException("bucket must be between 0 and ticks per second")
        } else {
            val numBuckets = DHServer.ticksPerSecond
            return shipMap.entries.asSequence().filter{(it.key.hashCode() % numBuckets) == bucket }.map{it.value}
        }
    }

    fun process(deltaSeconds: Double) {
        shipsToRemove.forEach { shipMap.remove(it.uuid) }
        shipsToRemove.clear()
        shipsToAdd.forEach { shipMap.put(it.uuid, it) }
        shipsToAdd.clear()
        getShips().forEach { it.process(deltaSeconds) }
    }

    fun markForAdd(ship: Ship) {
        shipsToAdd.add(ship)
    }

    fun markForRemove(ship: Ship) {
        shipsToRemove.add(ship)
    }
}