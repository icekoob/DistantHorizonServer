package com.dibujaron.distanthorizon

import com.dibujaron.distanthorizon.database.DhDatabase
import com.dibujaron.distanthorizon.database.impl.ExDatabase
import com.dibujaron.distanthorizon.login.PendingLoginManager
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
    val FACTORS_OF_CYCLE_LENGTH = factors(CYCLE_LENGTH_TICKS)

    const val REQUEST_BATCHING = true

    const val DEFAULT_BALANCE = 1000

    private var shuttingDown = false
    val serverProperties: Properties = loadProperties()
    val serverPort = serverProperties.getProperty("server.port", "25611").toInt()
    val serverSecret = serverProperties.getProperty("server.secret", "debug")
    val shipHeartbeatsEvery = serverProperties.getProperty("heartbeats.ship", "30").toInt()
    val shipHeartbeatsTickOffset = serverProperties.getProperty("heartbeats.ship.offset", "0").toInt()
    val worldHeartbeatsEvery = serverProperties.getProperty("heartbeats.world", "60").toInt()
    val worldHeartbeatsTickOffset = serverProperties.getProperty("heartbeats.world.offset", "0").toInt()
    val playerStartingShip = serverProperties.getProperty("starting.ship", "rijay.mockingbird")
    val startingPlanetName = serverProperties.getProperty("starting.planet", "Rakuri")
    val startingOrbitalRadius = serverProperties.getProperty("starting.radius", "400.0").toDouble()
    val startingOrbitalSpeed = serverProperties.getProperty("starting.speed", "25.0").toDouble()
    val dockingSpeed = serverProperties.getProperty("docking.speed", "200.0").toDouble()
    val dockingDist = serverProperties.getProperty("docking.distance", "200.0").toDouble()
    var debug = serverProperties.getProperty("debug", "true").toBoolean()
    val dbUrl = serverProperties.getProperty("database.url", "jdbc:postgresql://localhost/distant_horizon?user=postgres&password=admin")
    val dbDriver = serverProperties.getProperty("database.driver", "org.postgresql.Driver")

    private val javalin = initJavalin(serverPort)
    val timer =
        fixedRateTimer(name = "mainThread", initialDelay = TICK_LENGTH_MILLIS_CEIL, period = TICK_LENGTH_MILLIS_CEIL) { mainLoop() }

    private val database: DhDatabase = ExDatabase(dbUrl, dbDriver)
    private var tickCount = 0

    fun getDatabase(): DhDatabase
    {
        return database
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
        val isWorldStateMessageTick = tickCount % worldHeartbeatsEvery == worldHeartbeatsTickOffset
        if (isWorldStateMessageTick) {
            val worldStateMessage = composeWorldStateMessage()
            PlayerManager.getPlayers().forEach { it.queueWorldStateMsg(worldStateMessage) }
        }
        val isShipStateMessageTick = tickCount % shipHeartbeatsEvery == shipHeartbeatsTickOffset
        if (isShipStateMessageTick) {
            val shipHeartbeatsMessage = composeShipHeartbeatsMessageForAll()
            PlayerManager.getPlayers().forEach { it.queueShipHeartbeatsMsg(shipHeartbeatsMessage) }
        }
        PlayerManager.tick()
        tickCount++
    }

    //gotta get rid of the confirmation step. Also token should be ageless, or long-lived.
    fun initJavalin(port: Int): Javalin {
        println("initializing javalin on port $port")
        return Javalin.create { config ->
            config.defaultContentType = "application/json"
            config.autogenerateEtags = true
            config.asyncRequestTimeout = 10_000L
            config.enforceSsl = false
            config.showJavalinBanner = false
        }.ws("/ws/") { ws ->
                ws.onConnect { onClientConnect(it) }
                ws.onClose { onClientDisconnect(it) }
                ws.onBinaryMessage { onMessageReceived(it) }
                ws.onError { onSocketError(it) }
        }.get("/:serverSecret/prepLogin/:username"){
            if(verifySecret(it.pathParam("serverSecret"))) {
                val username = it.pathParam("username")
                val token = PendingLoginManager.registerPendingLoginGenerateToken(username)
                val db = database.getPersistenceDatabase()
                val acct = db.selectOrCreateAccount(username)
                val json = acct.toJSON()
                json.put("token", token)
                it.result(json.toString())
            }
        }.get("/:serverSecret/account/:accountName") {
            if(verifySecret(it.pathParam("serverSecret"))) {
                val acctName = it.pathParam("accountName")
                val dbInfo = database.getPersistenceDatabase().selectOrCreateAccount(acctName)
                println("Getting account data for account $acctName")
                it.result(dbInfo.toJSON().toString())
            }
        }.post("/:serverSecret/account/:accountName/createActor"){
            if(verifySecret(it.pathParam("serverSecret"))) {
                val acctName = it.pathParam("accountName")
                val db = database.getPersistenceDatabase()
                println("create actor request: " + it.body())
                val body = JSONObject(it.body())
                val displayName = body.getString("display_name")
                println("Creating actor for account $acctName with name $displayName")
                val acct = db.selectOrCreateAccount(acctName)
                db.createNewActorForAccount(acct, displayName)
                it.result(db.selectOrCreateAccount(acctName).toJSON().toString())
            }
        }.post("/:serverSecret/account/:accountName/deleteActor"){
            if(verifySecret(it.pathParam("serverSecret"))) {
                val acctName = it.pathParam("accountName")
                val body = JSONObject(it.body())
                val displayName = body.getString("display_name")
                println("Deleting actor $displayName from account $acctName")
                val db = database.getPersistenceDatabase()
                val acct = db.selectOrCreateAccount(acctName)
                for (actor in acct.actors) {
                    if (actor.displayName == displayName) {
                        db.deleteActor(actor)
                    }
                }
                it.result(db.selectOrCreateAccount(acctName).toJSON().toString())
            }
        }.start(port)
    }

    private fun verifySecret(clientSecret: String): Boolean
    {
        if(!debug && serverSecret == "debug"){
            throw IllegalStateException("Server is not in debug mode yet no server secret is set!")
        } else {
            val retval = serverSecret == clientSecret
            if(!retval){
                println("Warning: illegal client secret provided $clientSecret")
            }
            return retval
        }
    }

    private fun onClientConnect(conn: WsConnectContext) {
        val player = Player(conn)
        PlayerManager.addPlayer(player)
    }

    private fun onClientDisconnect(conn: WsCloseContext) {
        val player = PlayerManager.getPlayerByConnection(conn)
        if (player == null) {
            throw IllegalStateException("Connection disconnected but no player found for this connection.")
        } else {
            PlayerManager.removePlayer(player)
            val playerShip: Ship = player.ship
            ShipManager.removeShip(playerShip)
        }
    }

    private fun onMessageReceived(conn: WsBinaryMessageContext) {
        val player = PlayerManager.getPlayerByConnection(conn)
        if (player == null) {
            throw IllegalStateException("Connection received message but no player found for this connection.")
        } else {
            val messageStr = conn.data().toString(Charsets.UTF_8)
            val json = JSONObject(messageStr)
            player.queueIncomingMessageFromClient(json)
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
        PlayerManager.getPlayers().forEach { it.queueShipDockedMsg(dockedMessage) }
    }

    fun broadcastShipUndocked(ship: Ship) {
        val undockedMessage = ship.createShipHeartbeatJSON()
        PlayerManager.getPlayers().forEach { it.queueShipUndockedMsg(undockedMessage) }
    }

    private fun onSocketError(conn: WsErrorContext) {
        val player = PlayerManager.getPlayerByConnection(conn)
        if (player == null) {
            println("Connection error thrown and no player found for connection")
            throw conn.error()!!
        } else {
            println("Connection error for player id=${player.getDisplayName()}.")
            PlayerManager.removePlayer(player)
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

    private fun factors(num: Int): TreeSet<Int>
    {
        //https://stackoverflow.com/questions/47030439/get-factors-of-numbers-in-kotlin
        val factors = TreeSet<Int>()
        if (num < 1)
            return factors
        (1..num / 2)
            .filter { num % it == 0 }
            .forEach { factors.add(it) }
        factors.add(num)
        return factors
    }
}