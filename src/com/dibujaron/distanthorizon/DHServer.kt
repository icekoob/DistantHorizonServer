package com.dibujaron.distanthorizon

import com.dibujaron.distanthorizon.docking.DockingPort
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipManager
import io.javalin.Javalin
import io.javalin.websocket.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.lang.IllegalStateException
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
    public val startTime = System.currentTimeMillis()

    //private const val tickLengthNanos = (tickLengthSeconds * 1000000000).toLong()
    private var shuttingDown = false
    public var debug = false
    public var retrainsThisTick = 0
    var docksThisTick = 0
    var undocksThisTick = 0
    val serverProperties: Properties = loadProperties()
    private val javalin = initJavalin(serverProperties.getProperty("server.port", "25611").toInt())
    val playerStartingShip = serverProperties.getProperty("defaults.ship", "rijay.mockingbird")
    val timer =
        fixedRateTimer(name = "mainThread", initialDelay = TICK_LENGTH_MILLIS, period = TICK_LENGTH_MILLIS) { tick() }
    var lastTickTime = 0L
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

    var previousTickTime = 0L
    private fun tick() {
        val startTime = System.currentTimeMillis()
        val deltaSeconds = (startTime - lastTickTime) / 1000.0
        lastTickTime = startTime
        OrbiterManager.process(deltaSeconds)
        val isWorldStateMessageTick = tickCount % 50 == 0
        val isShipStateMessageTick = tickCount % 50 == 25
        val t1 = System.currentTimeMillis()
        ShipManager.process(deltaSeconds, !isWorldStateMessageTick && !isShipStateMessageTick)
        val t2 = System.currentTimeMillis()
        //send messages
        if (isWorldStateMessageTick) {
            val worldStateMessage = composeWorldStateMessage()
            PlayerManager.getPlayers().forEach { it.sendWorldState(worldStateMessage) }
        }
        val t3 = System.currentTimeMillis();
        if (isShipStateMessageTick) {
            val shipHeartbeatsMessage = composeShipHeartbeatsMessageForAll()
            PlayerManager.getPlayers().forEach { it.sendShipHeartbeats(shipHeartbeatsMessage) }
        }
        val t4 = System.currentTimeMillis();
        PlayerManager.process()
        val t5 = System.currentTimeMillis();
        val totalTimeMillis = System.currentTimeMillis() - startTime
        if (totalTimeMillis > TICK_LENGTH_MILLIS) {
            if (debug) {
                println("Orbiter manager processing: ${t1 - startTime}")
                println("Ship processing: ${t2 - t1}")
                println("World state messages: ${t3 - t2}")
                println("Ship heartbeat messages: ${t4 - t3}")
                println("Player processing: ${t5 - t4}")
                println("Retrains this tick: $retrainsThisTick")
                println("docks this tick: $docksThisTick")
                println("undocks this tick: $undocksThisTick")
            }
            if(previousTickTime > TICK_LENGTH_MILLIS){
                println("Warning, slow ticks: this tick $totalTimeMillis ms, previous tick $previousTickTime ms, limit is 20 ms")
            }
        }
        retrainsThisTick = 0
        docksThisTick = 0
        undocksThisTick = 0
        previousTickTime = totalTimeMillis
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

    private fun composeShipHeartbeatsMessageForTick(tickWithinSecond: Int): JSONArray {
        val ships = JSONArray()
        ShipManager.getShipsInBucket(tickWithinSecond).map { it.createShipHeartbeatJSON() }.forEach { ships.put(it) }
        return ships
    }

    fun composeInitialShipsMessage(): JSONArray {
        val ships = JSONArray()
        ShipManager.getShips().asSequence().map { it.createFullShipJSON() }.forEach { ships.put(it) }
        return ships
    }

    fun broadcastShipDocked(ship: Ship, shipPort: ShipDockingPort, station: Station, stationPort: DockingPort) {
        docksThisTick++
        val dockedMessage = JSONObject()
        dockedMessage.put("id", ship.uuid)
        dockedMessage.put("station_identifying_name", station.name)
        dockedMessage.put("ship_port", shipPort.toJSON())
        dockedMessage.put("station_port", stationPort.toJSON());
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

    fun timeSinceStart(): Long {
        return System.currentTimeMillis() - startTime
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