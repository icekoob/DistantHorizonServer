package com.dibujaron.distanthorizon.player

import com.dibujaron.distanthorizon.DHServer
import io.javalin.websocket.WsContext
import org.json.JSONArray
import org.json.JSONObject
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

    fun process()
    {
        playersToRemove.forEach{
            idMap.remove(it.uuid)
            connectionMap.remove(it.connection)
        }
        playersToRemove.clear()
        playersToAdd.forEach{
            idMap[it.uuid] = it
            connectionMap[it.connection] = it
            val worldStateMessage = DHServer.composeWorldStateMessage()
            it.sendWorldState(worldStateMessage)
            val shipsMessage = DHServer.composeInitialShipsMessage()
            it.sendInitialShipsState(shipsMessage)
        }
        playersToAdd.clear()
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