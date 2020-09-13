package com.dibujaron.distanthorizon.database.impl

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.database.ScriptDatabase
import com.dibujaron.distanthorizon.database.ScriptReader
import com.dibujaron.distanthorizon.database.ScriptWriter
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipInputs
import com.dibujaron.distanthorizon.ship.ShipState
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

//database impl using JetBrains Exposed
class ExposedDatabase(databaseUrl: String, databaseDriver: String) : ScriptDatabase() {

    object Route : IntIdTable("route") {
        val originStation: Column<String> =
            varchar("origin_station", 32) //todo make these foreign keys to a stations table?
        val destinationStation: Column<String> = varchar("dest_station", 32)
        val departureTick: Column<Int> = integer("departure_tick")
        val duration: Column<Int> = integer("duration")
        val startingLocationX: Column<Double> = double("start_loc_x")
        val startingLocationY: Column<Double> = double("start_loc_y")
        val startingRotation: Column<Double> = double("start_rotation")
        val startingVelocityX: Column<Double> = double("start_vel_x")
        val startingVelocityY: Column<Double> = double("start_vel_y")
        val mainThrustPower: Column<Double> = double("main_thrust")
        val manuThrustPower: Column<Double> = double("manu_thrust")
        val rotationPower: Column<Double> = double("rotation_power")
    }

    object RouteStep : IntIdTable("route_step") {
        val routeID = reference("route_id", Route.id)
        val stepTick: Column<Int> = integer("step_tick")
        val mainEngines: Column<Boolean> = bool("main_engines")
        val tillerLeft: Column<Boolean> = bool("tiller_left")
        val tillerRight: Column<Boolean> = bool("tiller_right")
        val portThrusters: Column<Boolean> = bool("port_thrust")
        val stbdThrusters: Column<Boolean> = bool("stbd_thrust")
        val foreThrusters: Column<Boolean> = bool("fore_thrust")
        val aftThrusters: Column<Boolean> = bool("aft_thrust")
    }

    init {
        val result = Database.connect(databaseUrl, driver = databaseDriver)
        println("Routes database connected. Dialect: ${result.dialect.name}, DB Version: ${result.version}, Database Name: ${result.name}. ")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Route)
            SchemaUtils.createMissingTablesAndColumns(RouteStep)
        }
        println("Routes schema and tables initialized.")
        transaction {
            println("There are ${Route.selectAll().count()} saved routes.")
        }
    }

    override fun selectStationsWithScripts(): List<Station> {
        return transaction {
            Route.slice(Route.originStation).selectAll()
                .withDistinct()
                .map { it[Route.originStation] }
                .mapNotNull { OrbiterManager.getStation(it) }
                .toList()
        }
    }

    override fun selectScriptsForStation(sourceStation: Station): List<ScriptReader> {
        val originStationFilter = (Route.originStation eq sourceStation.name)
        return transaction {
            Route.select { originStationFilter }
                .map { RelationalScriptReader(it) }
        }
    }

    //for now, the best script is the one with the earliest arrival date.
    override fun selectAvailableScript(
        sourceStation: Station,
        targetStation: Station,
        earliestDepartureTick: Int,
        latestDepartureTick: Int
    ): ScriptReader? {

        val originStationFilter = (Route.originStation eq sourceStation.name)
        val destStationFilter = (Route.destinationStation eq targetStation.name)
        val stationFilters = originStationFilter and destStationFilter
        val departureTickLowerLimit = (Route.departureTick greater earliestDepartureTick)
        val departureTickUpperLimit = (Route.departureTick less latestDepartureTick)
        val timeFilters = departureTickLowerLimit and departureTickUpperLimit
        val routeFilters = stationFilters and timeFilters

        //todo this is not ideal! don't use yet!
        val route = transaction {
            Route.select { routeFilters }.orderBy(Route.duration to SortOrder.ASC)
                .limit(1)
                .first()
        }

        return RelationalScriptReader(route)
    }

    override fun selectAvailableScriptToAnywhere(
        sourceStation: Station,
        earliestDepartureTick: Int,
        latestDepartureTick: Int
    ): ScriptReader? {
        val originStationFilter = (Route.originStation eq sourceStation.name)
        val departureTickLowerLimit = (Route.departureTick greater earliestDepartureTick)
        val departureTickUpperLimit = (Route.departureTick less latestDepartureTick)
        val timeFilters = departureTickLowerLimit and departureTickUpperLimit
        val routeFilters = originStationFilter and timeFilters

        val route = transaction {
            Route.select { routeFilters }
                .orderBy(Route.departureTick to SortOrder.ASC) //leave asap don't care where
                .limit(1)
                .first()
        }

        return RelationalScriptReader(route)
    }

    inner class RelationalScriptReader(private val steps: TreeMap<Int, ResultRow>, private val route: ResultRow) : ScriptReader {

        //private val steps: TreeMap<Int, ResultRow> = TreeMap()

        constructor(route: ResultRow): this(TreeMap(), route)

        init {
            if(steps.isEmpty()) {
                transaction { RouteStep.select { RouteStep.routeID eq route[Route.id].value } }
                    .forEach { steps[it[RouteStep.stepTick]] = it }
            }
        }

        override fun copy(): ScriptReader {
            return RelationalScriptReader(steps, route) //this does selects twice but I don't think I care.
        }

        override fun getDepartureTick(): Int {
            return route[Route.departureTick]
        }

        override fun getStartingState(): ShipState {
            return ShipState(
                Vector2(route[Route.startingLocationX], route[Route.startingLocationY]),
                route[Route.startingRotation],
                Vector2(route[Route.startingVelocityX], route[Route.startingVelocityY])
            )
        }

        override fun getMainThrustPower(): Double {
            return route[Route.mainThrustPower]
        }

        override fun getManuThrustPower(): Double {
            return route[Route.manuThrustPower]
        }

        override fun getRotationPower(): Double {
            return route[Route.rotationPower]
        }

        override fun hasNextAction(): Boolean {
            return steps.isNotEmpty()
        }

        override fun nextActionShouldFire(): Boolean {
            val nextActionTick = steps.firstKey()
            val currentTick = DHServer.getCurrentTickInCycle()
            if (nextActionTick < currentTick) {
                throw IllegalStateException("called nextActionReady but next action is in the past - must be called every tick!")
            } else {
                return nextActionTick == currentTick
            }
        }

        override fun getNextAction(): ShipInputs {
            if (!nextActionShouldFire()) {
                throw IllegalStateException("Called getNextAction() without nextActionShouldFire() being true!")
            } else {
                val step = steps.remove(steps.firstKey())
                if (step == null) {
                    throw IllegalStateException("First step is null")
                } else {
                    return ShipInputs(
                        step[RouteStep.mainEngines],
                        step[RouteStep.portThrusters],
                        step[RouteStep.stbdThrusters],
                        step[RouteStep.foreThrusters],
                        step[RouteStep.aftThrusters],
                        step[RouteStep.tillerLeft],
                        step[RouteStep.tillerRight]
                    )
                }
            }
        }
    }

    override fun beginLoggingScript(
        sourceStation: Station,
        startState: ShipState,
        shipClass: ShipClass
    ): ScriptWriter {
        println("Beginning script logging.")
        return RelationalScriptWriter(sourceStation, startState, shipClass)
    }

    inner class RelationalScriptWriter(
        private val sourceStation: Station,
        private val startState: ShipState,
        private val shipClass: ShipClass,
        private val startTick: Int = DHServer.getCurrentTickInCycle(),
        private val startTickAbsolute: Int = DHServer.getCurrentTickAbsolute()
    ) : ScriptWriter {
        private val steps: TreeMap<Int, ShipInputs> = TreeMap()

        override fun writeAction(action: ShipInputs) {
            steps[DHServer.getCurrentTickInCycle()] = action
        }

        //todo make not blocking if required
        override fun completeScript(dockedToStation: Station) {
            println("Saving script...")
            transaction {
                val newRouteId = Route.insertAndGetId {
                    it[originStation] = sourceStation.name
                    it[destinationStation] = dockedToStation.name
                    it[departureTick] = startTick
                    it[duration] = DHServer.getCurrentTickAbsolute() - startTickAbsolute
                    it[startingLocationX] = startState.position.x
                    it[startingLocationY] = startState.position.y
                    it[startingRotation] = startState.rotation
                    it[startingVelocityX] = startState.velocity.x
                    it[startingVelocityY] = startState.velocity.y
                    it[mainThrustPower] = shipClass.mainThrust
                    it[manuThrustPower] = shipClass.manuThrust
                    it[rotationPower] = shipClass.rotationPower
                }

                RouteStep.batchInsert(steps.entries) { entry ->
                    val tick = entry.key
                    val inputs = entry.value
                    this[RouteStep.routeID] = newRouteId
                    this[RouteStep.stepTick] = tick
                    this[RouteStep.mainEngines] = inputs.mainEnginesActive
                    this[RouteStep.tillerLeft] = inputs.tillerLeft
                    this[RouteStep.tillerRight] = inputs.tillerRight
                    this[RouteStep.portThrusters] = inputs.portThrustersActive
                    this[RouteStep.stbdThrusters] = inputs.stbdThrustersActive
                    this[RouteStep.foreThrusters] = inputs.foreThrustersActive
                    this[RouteStep.aftThrusters] = inputs.aftThrustersActive
                }
            }
            println("Script saved, step count is ${steps.size}.")
        }
    }

    override fun shutdown() {
        return
    }


}