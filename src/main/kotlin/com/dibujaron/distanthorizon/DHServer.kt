package com.dibujaron.distanthorizon

import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.script.impl.relational.RelationalScriptDatabase
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipManager
import io.javalin.Javalin
import io.javalin.websocket.WsBinaryMessageContext
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsConnectContext
import io.javalin.websocket.WsErrorContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.thread
import kotlin.math.ceil

fun main() {
    thread { DHServer.commandLoop() }
}

object DHServer {

    const val TICK_LENGTH_SECONDS = 1.0/60.0
    const val TICK_LENGTH_MILLIS = 1000.0/60.0
    val TICK_LENGTH_MILLIS_CEIL = ceil(TICK_LENGTH_MILLIS).toLong()
    const val TICKS_PER_SECOND = 60

    const val CYCLE_LENGTH_TICKS = 83160/*TICKS_PER_SECOND * 60 * 12  //cycle every 12m*/

    const val WORLD_HEARTBEATS_EVERY = 60
    const val WORLD_HEARTBEAT_TICK_OFFSET = 0

    const val SHIP_HEARTBEATS_EVERY = 60
    const val SHIP_HEARTBEAT_TICK_OFFSET = 30

    private var shuttingDown = false
    var debug = false
    val serverProperties: Properties = loadProperties()
    private val javalin = initJavalin(serverProperties.getProperty("server.port", "25611").toInt())
    val playerStartingShip = serverProperties.getProperty("defaults.ship", "rijay.mockingbird")
    val dockingSpeed = serverProperties.getProperty("docking.speed", "500.0").toDouble()
    val dockingDist = serverProperties.getProperty("docking.distance", "500.0").toDouble()
    val timer =
        fixedRateTimer(name = "mainThread", initialDelay = TICK_LENGTH_MILLIS_CEIL, period = TICK_LENGTH_MILLIS_CEIL) { mainLoop() }

    private val scriptDatabase = RelationalScriptDatabase("jdbc:postgresql://localhost/routes?user=postgres&password=admin", "org.postgresql.Driver")
    private var tickCount = 0

    fun getScriptDatabase(): RelationalScriptDatabase
    {
        return scriptDatabase
    }

    fun getCurrentTickAbsolute(): Int {
        return tickCount
    }

    fun getCurrentTickInCycle(): Int {
        return tickCount % CYCLE_LENGTH_TICKS
    }

    fun commandLoop() {
        while (!shuttingDown) {
            val command = readLine()
            if (command == "stop") {
                shuttingDown = true;
                timer.cancel()
                javalin.stop()
            } else if (command == "debug") {
                debug = !debug;
                println("debug: $debug")
            } else {
                println("Unknown command $command")
            }
        }
    }

    var lastTickTime = System.currentTimeMillis()
    var accumulator = 0.0

    private fun mainLoop() {
        val tickTime = System.currentTimeMillis()
        val delta = tickTime - lastTickTime
        accumulator += delta
        var count = 0
        while(accumulator >= TICK_LENGTH_MILLIS){
            tick()
            count++
            accumulator -= TICK_LENGTH_MILLIS
        }
        lastTickTime = tickTime
    }

    private fun tick() {
        OrbiterManager.tick()
        ShipManager.tick()
        val isWorldStateMessageTick = tickCount % WORLD_HEARTBEATS_EVERY == WORLD_HEARTBEAT_TICK_OFFSET
        if (isWorldStateMessageTick) {
            val worldStateMessage = composeWorldStateMessage()
            PlayerManager.getPlayers().forEach { it.sendWorldState(worldStateMessage) }
        }
        val isShipStateMessageTick = tickCount % SHIP_HEARTBEATS_EVERY == SHIP_HEARTBEAT_TICK_OFFSET
        if (isShipStateMessageTick) {
            val shipHeartbeatsMessage = composeShipHeartbeatsMessageForAll()
            PlayerManager.getPlayers().forEach { it.sendShipHeartbeats(shipHeartbeatsMessage) }
        }
        PlayerManager.tick()
        tickCount++
    }

    fun initJavalin(port: Int): Javalin {
        println("initializing javalin on port $port")
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
            }.start(port)
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

    fun composeWorldStateMessage(): JSONObject {
        val worldStateMessage = JSONObject()
        val planets = JSONArray()
        OrbiterManager.getPlanets().asSequence().map { it.createOrbiterJson() }.forEach { planets.put(it) }
        worldStateMessage.put("planets", planets)

        val stations = JSONArray()
        OrbiterManager.getStations().asSequence().map { it.createOrbiterJson() }.forEach { stations.put(it) }
        worldStateMessage.put("stations", stations)
        return worldStateMessage
    }

    private fun composeShipHeartbeatsMessageForAll(): JSONArray {
        val ships = JSONArray()
        ShipManager.getShips().map { it.createShipHeartbeatJSON() }.forEach { ships.put(it) }
        return ships
    }

    fun composeMessageForShipsAdded(inputShips: Collection<Ship>): JSONArray
    {
        val outputShips = JSONArray()
        inputShips.asSequence().map{it.createFullShipJSON()}.forEach { outputShips.put(it) }
        return outputShips
    }

    fun composeMessageForShipsRemoved(inputShips: Collection<Ship>): JSONArray
    {
        val outputShips = JSONArray()
        inputShips.asSequence().map{it.uuid}.forEach { outputShips.put(it) }
        return outputShips
    }

    fun broadcastShipDocked(ship: Ship) {
        val dockedMessage = ship.createDockedMessage()
        PlayerManager.getPlayers().forEach { it.sendShipDocked(dockedMessage) }
    }

    fun broadcastShipUndocked(ship: Ship) {
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

    fun loadProperties(): Properties {
        val p = Properties()
        val f = File("./server.properties")
        if(f.exists()) {
            println("Found server properties file.")
            p.load(FileReader(f))
        } else {
            println("Found no server properties file, using defaults.")
        }
        return p
    }

    fun ticksToSeconds(ticks: Double): Double
    {
        return ticks / TICKS_PER_SECOND
    }

    fun secondsToTicks(seconds: Double): Double
    {
        return seconds * TICKS_PER_SECOND
    }
}