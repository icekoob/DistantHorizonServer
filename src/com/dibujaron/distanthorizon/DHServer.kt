package com.dibujaron.distanthorizon

import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipManager
import io.javalin.Javalin
import io.javalin.websocket.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.IllegalStateException
import kotlin.concurrent.thread

fun main() {
    thread{DHServer.commandLoop()}
    DHServer.run()
}

object DHServer {

    private const val tickLengthMs = 17
    private const val tickLengthSeconds = tickLengthMs / 1000.0
    private var shuttingDown = false

    fun commandLoop(){
        while(!shuttingDown) {
            val command = readLine()
            if(command == "stop"){
                shuttingDown = true;
            } else {
                println("Unknown command $command")
            }
        }
    }

    fun run() {
        val jav = initJavalin()
        var deltaSeconds = 0.0
        var firstTick = true;
        while (!shuttingDown) {
            //process
            val startTime = System.currentTimeMillis()
            var priorTime = startTime
            OrbiterManager.process(deltaSeconds)
            val orbiterTime = System.currentTimeMillis() - priorTime
            priorTime = System.currentTimeMillis()
            ShipManager.process(deltaSeconds)
            val shipsTime = System.currentTimeMillis() - priorTime
            priorTime = System.currentTimeMillis()
            //send messages
            val worldStateMessage = composeWorldStateMessage()
            val composeTime = System.currentTimeMillis() - priorTime
            priorTime = System.currentTimeMillis()
            PlayerManager.process(worldStateMessage)
            val playerTime = System.currentTimeMillis() - priorTime
            val totalTime = System.currentTimeMillis() - startTime

            //if we have time left over, sleep.
            if (totalTime < tickLengthMs) {
                Thread.sleep(tickLengthMs - totalTime)
                deltaSeconds = tickLengthSeconds
            } else {
                if(!firstTick) {
                    println("can't keep up! Tick took ${totalTime}ms")
                    println("    orbiter processing: ${orbiterTime}ms")
                    println("    ships processing: ${shipsTime}ms")
                    println("    compose message: ${composeTime}ms")
                    println("    player processing: ${playerTime}ms")
                }
                deltaSeconds = totalTime / 1000.0
            }
            firstTick = false;
        }
        jav.stop()
    }

    fun initJavalin(): Javalin {
        return Javalin.create { config ->
            config.defaultContentType = "application/json"
            config.autogenerateEtags = true
            config.asyncRequestTimeout = 10_000L
            config.enforceSsl = true
        }
            .ws("/ws/") { ws ->
                ws.onConnect { onClientConnect(it) }
                ws.onClose { onClientDisconnect(it) }
                ws.onBinaryMessage() { onMessageReceived(it) }
                ws.onError { onSocketError(it) }
            }.start(25611)
    }

    private fun onClientConnect(conn: WsConnectContext) {
        val player = Player(conn)
        PlayerManager.markForAdd(player)
        println("Player id=${player.uuid} joined the game")
    }

    private fun onClientDisconnect(conn: WsCloseContext) {
        val player = PlayerManager.getPlayerByConnection(conn)
        if (player == null) {
            throw IllegalStateException("Connection disconnected but no player found for this connection.")
        } else {
            PlayerManager.markForRemove(player)
            var playerShip: Ship = player.myShip
            ShipManager.markForRemove(playerShip)
            println("Player id=${player.uuid} left the game")
        }
    }

    private fun onMessageReceived(conn: WsBinaryMessageContext) {
        val player = PlayerManager.getPlayerByConnection(conn)
        if (player == null) {
            throw IllegalStateException("Connection received message but no player found for this connection.")
        } else {
            val messageStr = conn.data().toString(Charsets.UTF_8)
            val json = JSONObject(messageStr)
            player.onMessageFromClient(json)
        }
    }

    private fun composeWorldStateMessage(): JSONObject
    {
        val worldStateMessage = JSONObject()
        val planets = JSONArray()
        OrbiterManager.getPlanets().asSequence().map { it.toJSON() }.forEach { planets.put(it) }
        worldStateMessage.put("planets", planets)

        val stations = JSONArray()
        OrbiterManager.getStations().asSequence().map { it.toJSON() }.forEach { stations.put(it) }
        worldStateMessage.put("stations", stations)

        val ships = JSONArray()
        ShipManager.getShips().asSequence().map { it.toJSON() }.forEach { ships.put(it) }
        worldStateMessage.put("ships", ships)
        return worldStateMessage
    }

    private fun onSocketError(conn: WsErrorContext) {
        val player = PlayerManager.getPlayerByConnection(conn)
        if (player == null) {
            println("Connection error thrown and no player found for connection")
            throw conn.error()!!
        } else {
            println("Connection error for player id=${player.uuid}.")
            PlayerManager.markForRemove(player)
        }
    }
}