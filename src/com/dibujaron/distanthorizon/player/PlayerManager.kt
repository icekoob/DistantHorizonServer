package com.dibujaron.distanthorizon.player

import io.javalin.websocket.WsContext
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

    fun process(worldStateMessage: JSONObject)
    {
        playersToRemove.forEach{
            idMap.remove(it.uuid)
            connectionMap.remove(it.connection)
        }
        playersToRemove.clear()
        playersToAdd.forEach{
            idMap[it.uuid] = it
            connectionMap[it.connection] = it
        }
        playersToAdd.clear()
        getPlayers().forEach{it.sendWorldState(worldStateMessage)}
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