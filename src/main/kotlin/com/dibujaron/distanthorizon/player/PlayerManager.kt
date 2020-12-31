package com.dibujaron.distanthorizon.player

import com.dibujaron.distanthorizon.timer.ScheduledTaskManager
import io.javalin.websocket.WsContext
import java.util.concurrent.ConcurrentHashMap

object PlayerManager {
    private val authenticatedUserMap: ConcurrentHashMap<String, Player> = ConcurrentHashMap()
    private val connectionMap: ConcurrentHashMap<WsContext, Player> = ConcurrentHashMap()

    fun addPlayer(player: Player) {
        connectionMap[player.connection] = player
        ScheduledTaskManager.runDelayed(5, { broadcast("- ${player.getDisplayName()} joined the game -") })
    }

    fun mapAuthenticatedPlayer(username: String, player: Player) {
        authenticatedUserMap[username] = player
    }

    fun removePlayer(player: Player) {
        connectionMap.remove(player.connection)
        if (player.isAuthenticated()) {
            authenticatedUserMap.remove(player.getUsername())
            broadcast("- ${player.getDisplayName()} left the game -")
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
        println("$senderName: $message")
        getPlayers().forEach { it.queueChatMsg(senderName, message) }
    }

    fun broadcast(message: String) {
        println(message)
        getPlayers().forEach { it.queueChatMsg(message) }
    }
}