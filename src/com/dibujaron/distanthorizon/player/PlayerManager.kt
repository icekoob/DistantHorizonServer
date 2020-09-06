package com.dibujaron.distanthorizon.player

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.ship.ShipManager
import io.javalin.websocket.WsContext
import java.util.*
import kotlin.collections.HashMap

object PlayerManager {
    private val idMap: HashMap<UUID, Player> = HashMap()
    private val connectionMap: HashMap<WsContext, Player> = HashMap()
    private val playersToAdd = LinkedList<Player>()
    private val playersToRemove = LinkedList<Player>()

    fun markForAdd(player: Player)
    {
        playersToAdd.add(player)
    }

    fun markForRemove(player: Player)
    {
        playersToRemove.add(player)
    }

    fun playerCount(): Int{
        return idMap.size - playersToRemove.size + playersToAdd.size
    }

    fun tick()
    {
        playersToRemove.forEach{
            idMap.remove(it.uuid)
            connectionMap.remove(it.connection)
        }
        playersToRemove.clear()
        if(!playersToAdd.isEmpty()) {
            val worldStateMessage = DHServer.composeWorldStateMessage()
            val shipsMessage = DHServer.composeMessageForShipsAdded(ShipManager.getShips())
            playersToAdd.forEach {
                idMap[it.uuid] = it
                connectionMap[it.connection] = it
                it.sendWorldState(worldStateMessage)
                it.sendShipsAdded(shipsMessage)
            }
            playersToAdd.clear()
        }
    }

    fun getPlayerById(uuid: UUID): Player?
    {
        return idMap[uuid]
    }

    fun getPlayerByConnection(conn: WsContext): Player?
    {
        return connectionMap[conn]
    }

    fun getPlayers(): Collection<Player>
    {
        return idMap.values
    }
}