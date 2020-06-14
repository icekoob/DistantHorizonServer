package com.dibujaron.distanthorizon

import com.dibujaron.distanthorizon.docking.DockingPort
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.Orbiter
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipClassManager
import com.dibujaron.distanthorizon.ship.ShipManager
import io.javalin.Javalin
import io.javalin.websocket.*
import org.json.JSONArray
import org.json.JSONObject
import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread
import kotlin.concurrent.timerTask
import kotlin.math.pow

fun main() {
    thread{DHServer.commandLoop()}
}

object DHServer {

    public const val tickLengthMillis = 20L
    public const val tickLengthSeconds = tickLengthMillis / 1000.0//0.016667
    public const val ticksPerSecond = 50
    public val startTime = System.currentTimeMillis()
    //private const val tickLengthNanos = (tickLengthSeconds * 1000000000).toLong()
    private var shuttingDown = false
    private val timer = fixedRateTimer(name="mainThread", initialDelay = tickLengthMillis, period= tickLengthMillis){tick()}
    private val javalin = initJavalin()
    var lastTickTime = 0L
    var tickCount = 0
    fun commandLoop(){
        while(!shuttingDown) {
            val command = readLine()
            if(command == "stop"){
                shuttingDown = true;
                timer.cancel()
                javalin.stop()
            } else {
                println("Unknown command $command")
            }
        }
    }

    private fun tick(){
        val startTime = System.currentTimeMillis()
        val deltaSeconds = (startTime - lastTickTime) / 1000.0
        lastTickTime = startTime
        OrbiterManager.process(deltaSeconds)
        ShipManager.process(deltaSeconds)
        //send messages
        if(tickCount % 50 == 0){
            val worldStateMessage = composeWorldStateMessage()
            PlayerManager.getPlayers().forEach{it.sendWorldState(worldStateMessage)}
        }
        val shipHeartbeatsMessage = composeShipHeartbeatsMessageForAll()
        PlayerManager.getPlayers().forEach { it.sendShipHeartbeats(shipHeartbeatsMessage) }
        PlayerManager.process()
        val totalTimeMillis = System.currentTimeMillis() - startTime
        if(totalTimeMillis > tickLengthMillis) {
            println("can't keep up! Tick took ${totalTimeMillis}ms, limit is $tickLengthMillis")
        }
        tickCount++
    }

    fun initJavalin(): Javalin {
        return Javalin.create { config ->
            config.defaultContentType = "application/json"
            config.autogenerateEtags = true
            config.asyncRequestTimeout = 10_000L
            config.enforceSsl = true
            config.showJavalinBanner = false
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
        println("Player id=${player.uuid} joined the game, player count is ${PlayerManager.playerCount()}")
    }

    private fun onClientDisconnect(conn: WsCloseContext) {
        val player = PlayerManager.getPlayerByConnection(conn)
        if (player == null) {
            throw IllegalStateException("Connection disconnected but no player found for this connection.")
        } else {
            PlayerManager.markForRemove(player)
            var playerShip: Ship = player.ship
            ShipManager.markForRemove(playerShip)
            println("Player id=${player.uuid} left the game, reason=${conn.reason()}. player count is ${PlayerManager.playerCount()}")
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

    fun composeWorldStateMessage(): JSONObject
    {
        val worldStateMessage = JSONObject()
        val planets = JSONArray()
        OrbiterManager.getPlanets().asSequence().map { it.createOrbiterJson() }.forEach { planets.put(it) }
        worldStateMessage.put("planets", planets)

        val stations = JSONArray()
        OrbiterManager.getStations().asSequence().map { it.createOrbiterJson() }.forEach { stations.put(it) }
        worldStateMessage.put("stations", stations)
        return worldStateMessage
    }

    private fun composeShipHeartbeatsMessageForAll(): JSONArray
    {
        val ships = JSONArray()
        ShipManager.getShips().map { it.createShipHeartbeatJSON() }.forEach { ships.put(it) }
        return ships
    }
    private fun composeShipHeartbeatsMessageForTick(tickWithinSecond: Int): JSONArray
    {
        val ships = JSONArray()
        ShipManager.getShipsInBucket(tickWithinSecond).map { it.createShipHeartbeatJSON() }.forEach { ships.put(it) }
        return ships
    }
    fun composeInitialShipsMessage(): JSONArray
    {
        val ships = JSONArray()
        ShipManager.getShips().asSequence().map { it.createFullShipJSON() }.forEach { ships.put(it) }
        return ships
    }

    fun broadcastShipDocked(ship: Ship, shipPort: ShipDockingPort, station: Station, stationPort: DockingPort)
    {
        val dockedMessage = JSONObject()
        dockedMessage.put("id", ship.uuid)
        dockedMessage.put("station_identifying_name", station.name)
        dockedMessage.put("ship_port", shipPort.toJSON())
        dockedMessage.put("station_port", stationPort.toJSON());
        PlayerManager.getPlayers().forEach { it.sendShipDocked(dockedMessage) }
    }

    fun broadcastShipUndocked(ship: Ship)
    {
        val undockedMessage = ship.createShipHeartbeatJSON()
        PlayerManager.getPlayers().forEach { it.sendShipUndocked(undockedMessage) }
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

    fun timeSinceStart(): Long{
        return System.currentTimeMillis() - startTime
    }
}