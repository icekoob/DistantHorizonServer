package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager
import java.util.*

object ShipManager {
    private val shipMap = HashMap<UUID, Ship>()
    private val shipsToAdd = LinkedList<Ship>()
    private val shipsToRemove = LinkedList<Ship>()

    fun getShips(): Collection<Ship>{
        return shipMap.values
    }

    fun process(deltaSeconds: Double)
    {
        shipsToRemove.forEach{shipMap.remove(it.uuid)}
        shipsToRemove.clear()
        shipsToAdd.forEach{shipMap.put(it.uuid, it)}
        shipsToAdd.clear()
        getShips().forEach { it.process(deltaSeconds) }
    }
    fun markForAdd(ship: Ship)
    {
        shipsToAdd.add(ship)
    }

    fun markForRemove(ship: Ship)
    {
        shipsToRemove.add(ship)
    }
}