package com.dibujaron.distanthorizon.player

import io.javalin.websocket.WsContext
import java.util.concurrent.ConcurrentHashMap

object PlayerManager {
    private val authenticatedUserMap: ConcurrentHashMap<String, Player> = ConcurrentHashMap()
    private val connectionMap: ConcurrentHashMap<WsContext, Player> = ConcurrentHashMap()

    fun addPlayer(player: Player) {
        connectionMap[player.connection] = player
    }

    fun mapAuthenticatedPlayer(username: String, player: Player){
        authenticatedUserMap[username] = player
    }

    fun removePlayer(player: Player) {
        connectionMap.remove(player.connection)
        if(player.isAuthenticated()){
            authenticatedUserMap.remove(player.getUsername())
        }
    }

    fun playerCount(): Int {
        return connectionMap.size
    }

    fun tick() {
        getPlayers(true).forEach { it.tick() }
    }

    fun getPlayerByUsername(username: String): Player? {
        return authenticatedUserMap[username]
    }

    fun getPlayerByConnection(conn: WsContext): Player? {
        return connectionMap[conn]
    }

    fun getPlayers(includeUninitialized: Boolean = false): Sequence<Player> {
        return connectionMap.values.asSequence().filter { includeUninitialized || it.initialized }
    }

    fun broadcast(senderName: String, message: String) {
        getPlayers().forEach { it.queueChatMsg(senderName, message) }
    }
}