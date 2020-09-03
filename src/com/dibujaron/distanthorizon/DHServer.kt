package com.dibujaron.distanthorizon

import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager
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

fun main() {
    thread { DHServer.commandLoop() }
}

object DHServer {

    public const val TICK_LENGTH_MILLIS = 20L
    public const val TICK_LENGTH_SECONDS = TICK_LENGTH_MILLIS / 1000.0//0.016667
    public const val TICKS_PER_SECOND = 50

    //private const val tickLengthNanos = (tickLengthSeconds * 1000000000).toLong()
    private var shuttingDown = false
    var debug = false
    var retrainsThisTick = 0
    var docksThisTick = 0
    var undocksThisTick = 0
    val serverProperties: Properties = loadProperties()
    private val javalin = initJavalin(serverProperties.getProperty("server.port", "25611").toInt())
    val playerStartingShip = serverProperties.getProperty("defaults.ship", "rijay.mockingbird")
    val dockingSpeed = serverProperties.getProperty("docking.speed", "500.0").toDouble()
    val dockingDist = serverProperties.getProperty("docking.distance", "500.0").toDouble()
    val timer =
        fixedRateTimer(name = "mainThread", initialDelay = TICK_LENGTH_MILLIS, period = TICK_LENGTH_MILLIS) { tick() }
    var tickCount = 0

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

    var lastTickTime = 0L
    private fun tick() {
        val tickStartTime = System.currentTimeMillis()
        val deltaSeconds = (tickStartTime - lastTickTime) / 1000.0
        lastTickTime = tickStartTime
        OrbiterManager.process(deltaSeconds)
        ShipManager.process(deltaSeconds)
        //send messages
        val isWorldStateMessageTick = tickCount % 50 == 0
        if (isWorldStateMessageTick) {
            val worldStateMessage = composeWorldStateMessage()
            PlayerManager.getPlayers().forEach { it.sendWorldState(worldStateMessage) }
        }
        val isShipStateMessageTick = tickCount % 50 == 25
        if (isShipStateMessageTick) {
            val shipHeartbeatsMessage = composeShipHeartbeatsMessageForAll()
            PlayerManager.getPlayers().forEach { it.sendShipHeartbeats(shipHeartbeatsMessage) }
        }
        PlayerManager.process()
        retrainsThisTick = 0
        docksThisTick = 0
        undocksThisTick = 0
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

    fun composeInitialShipsMessage(): JSONArray {
        return composeMessageForShipsAdded(ShipManager.getShips())
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
        docksThisTick++
        val dockedMessage = ship.createDockedMessage()
        PlayerManager.getPlayers().forEach { it.sendShipDocked(dockedMessage) }
    }

    fun broadcastShipUndocked(ship: Ship) {
        undocksThisTick++
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
}