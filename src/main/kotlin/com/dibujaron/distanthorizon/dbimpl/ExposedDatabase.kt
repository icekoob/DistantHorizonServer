package com.dibujaron.distanthorizon.dbimpl

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.script.ScriptDatabase
import com.dibujaron.distanthorizon.script.ScriptReader
import com.dibujaron.distanthorizon.script.ScriptWriter
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipClassManager
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
class ExposedDatabase(databaseUrl: String, databaseDriver: String) : ScriptDatabase {

    init {
        val result = Database.connect(databaseUrl, driver = databaseDriver)
        println("Routes database connected. Dialect: ${result.dialect.name}, DB Version: ${result.version}, Database Name: ${result.name}. ")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(RouteDBO)
            SchemaUtils.createMissingTablesAndColumns(RouteStepDBO)
        }
        println("Routes schema and tables initialized.")
        transaction {
            println("There are ${RouteDBO.selectAll().count()} saved routes.")
        }
    }
    //CLASSES
    object RouteDBO : IntIdTable("route") {
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
        val shipClass: Column<String> = varchar("ship_class", 32)
        val plottedByPlayer = reference("plotted_by_player_id", PlayerDBO.id).nullable()
    }

    object RouteStepDBO : IntIdTable("route_step") {
        val routeID = reference("route_id", RouteDBO.id)
        val stepTick: Column<Int> = integer("step_tick")
        val mainEngines: Column<Boolean> = bool("main_engines")
        val tillerLeft: Column<Boolean> = bool("tiller_left")
        val tillerRight: Column<Boolean> = bool("tiller_right")
        val portThrusters: Column<Boolean> = bool("port_thrust")
        val stbdThrusters: Column<Boolean> = bool("stbd_thrust")
        val foreThrusters: Column<Boolean> = bool("fore_thrust")
        val aftThrusters: Column<Boolean> = bool("aft_thrust")
    }

    object PlayerDBO: IntIdTable("player") {
        val balance: Column<Int> = integer("balance")
        val accountName: Column<String> = varchar("account_name", 32)
        val displayName: Column<String> = varchar("display_name", 32)
        val lastDockedStation: Column<String> = varchar("last_docked_station", 32)
        val currentShip = reference("current_ship_id", ShipDBO.id).nullable()
    }

    object StationDBO: IntIdTable("station") {
        val identifyingName: Column<String> = varchar("identifying_name", 32) //TODO convert entire system to DB instead of yml
    }

    object ShipDBO: IntIdTable("ship_instance") {
        val shipClass = reference("ship_class_id", ShipClassDBO.id)
        val ownedByPlayer = reference("owned_by_player_id", PlayerDBO.id)
        val storedAtStation = reference("stored_at_station", StationDBO.id)
        val primaryColor: Column<Int> = integer("primary_color")
        val secondaryColor: Column<Int> = integer("secondary_color")
    }

    object ShipClassDBO: IntIdTable("ship_class") {
        val identifyingName: Column<String> = varchar("identifying_name", 32) //TODO convert entire system to DB instead of yml
    }

    //SCRIPT DATABASE IMPL
    override fun selectStationsWithScripts(): List<Station> {
        return transaction {
            RouteDBO.slice(RouteDBO.originStation).selectAll()
                .withDistinct()
                .map { it[RouteDBO.originStation] }
                .mapNotNull { OrbiterManager.getStation(it) }
                .toList()
        }
    }

    override fun selectScriptsForStation(sourceStation: Station): List<ScriptReader> {
        val originStationFilter = (RouteDBO.originStation eq sourceStation.name)
        return transaction {
            RouteDBO.select { originStationFilter }
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

        val originStationFilter = (RouteDBO.originStation eq sourceStation.name)
        val destStationFilter = (RouteDBO.destinationStation eq targetStation.name)
        val stationFilters = originStationFilter and destStationFilter
        val departureTickLowerLimit = (RouteDBO.departureTick greater earliestDepartureTick)
        val departureTickUpperLimit = (RouteDBO.departureTick less latestDepartureTick)
        val timeFilters = departureTickLowerLimit and departureTickUpperLimit
        val routeFilters = stationFilters and timeFilters

        val routes = transaction {
            RouteDBO.select { routeFilters }.orderBy(RouteDBO.duration to SortOrder.ASC).toList()
        }

        val selectedRoute =
            routes.asSequence().minByOrNull { it[RouteDBO.departureTick] + it[RouteDBO.duration] } //min by arrival time
        return if (selectedRoute == null) {
            null
        } else {
            RelationalScriptReader(selectedRoute)
        }
    }

    override fun selectAvailableScriptToAnywhere(
        sourceStation: Station,
        earliestDepartureTick: Int,
        latestDepartureTick: Int
    ): ScriptReader? {
        val originStationFilter = (RouteDBO.originStation eq sourceStation.name)
        val departureTickLowerLimit = (RouteDBO.departureTick greater earliestDepartureTick)
        val departureTickUpperLimit = (RouteDBO.departureTick less latestDepartureTick)
        val timeFilters = departureTickLowerLimit and departureTickUpperLimit
        val routeFilters = originStationFilter and timeFilters

        val route = transaction {
            RouteDBO.select { routeFilters }
                .orderBy(RouteDBO.departureTick to SortOrder.ASC) //leave asap don't care where
                .limit(1)
                .first()
        }

        return RelationalScriptReader(route)
    }

    inner class RelationalScriptReader(private val steps: TreeMap<Int, ResultRow>, private val route: ResultRow) :
        ScriptReader {

        //private val steps: TreeMap<Int, ResultRow> = TreeMap()

        constructor(route: ResultRow) : this(TreeMap(), route)

        init {
            if (steps.isEmpty()) {
                transaction { RouteStepDBO.select { RouteStepDBO.routeID eq route[RouteDBO.id].value }
                    .forEach { steps[it[RouteStepDBO.stepTick]] = it }}
            }
        }

        override fun copy(): ScriptReader {
            return RelationalScriptReader(steps, route) //this does selects twice but I don't think I care.
        }

        override fun getDepartureTick(): Int {
            return route[RouteDBO.departureTick]
        }

        override fun getStartingState(): ShipState {
            return ShipState(
                Vector2(route[RouteDBO.startingLocationX], route[RouteDBO.startingLocationY]),
                route[RouteDBO.startingRotation],
                Vector2(route[RouteDBO.startingVelocityX], route[RouteDBO.startingVelocityY])
            )
        }

        override fun getShipClass(): ShipClass {
            val shipClassName = route[RouteDBO.shipClass]
            return ShipClassManager.getShipClass(shipClassName) ?: throw IllegalStateException("No ship class found $shipClassName")
        }

        override fun hasNextAction(): Boolean {
            return steps.isNotEmpty()
        }

        override fun nextActionShouldFire(): Boolean {
            val nextActionTick = steps.firstKey()
            val currentTick = DHServer.getCurrentTickInCycle()
            if (nextActionTick < currentTick) {
                throw IllegalStateException("next action is in the past? nat: $nextActionTick, ct: $currentTick, route: ${route[RouteDBO.id]}")
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
                        step[RouteStepDBO.mainEngines],
                        step[RouteStepDBO.portThrusters],
                        step[RouteStepDBO.stbdThrusters],
                        step[RouteStepDBO.foreThrusters],
                        step[RouteStepDBO.aftThrusters],
                        step[RouteStepDBO.tillerLeft],
                        step[RouteStepDBO.tillerRight]
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
        private val shipType: ShipClass,
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
                val newRouteId = RouteDBO.insertAndGetId {
                    it[originStation] = sourceStation.name
                    it[destinationStation] = dockedToStation.name
                    it[departureTick] = startTick
                    it[duration] = DHServer.getCurrentTickAbsolute() - startTickAbsolute
                    it[startingLocationX] = startState.position.x
                    it[startingLocationY] = startState.position.y
                    it[startingRotation] = startState.rotation
                    it[startingVelocityX] = startState.velocity.x
                    it[startingVelocityY] = startState.velocity.y
                    it[shipClass] = shipType.qualifiedName
                }

                RouteStepDBO.batchInsert(steps.entries) { entry ->
                    val tick = entry.key
                    val inputs = entry.value
                    this[RouteStepDBO.routeID] = newRouteId
                    this[RouteStepDBO.stepTick] = tick
                    this[RouteStepDBO.mainEngines] = inputs.mainEnginesActive
                    this[RouteStepDBO.tillerLeft] = inputs.tillerLeft
                    this[RouteStepDBO.tillerRight] = inputs.tillerRight
                    this[RouteStepDBO.portThrusters] = inputs.portThrustersActive
                    this[RouteStepDBO.stbdThrusters] = inputs.stbdThrustersActive
                    this[RouteStepDBO.foreThrusters] = inputs.foreThrustersActive
                    this[RouteStepDBO.aftThrusters] = inputs.aftThrustersActive
                }
            }
            println("Script saved, step count is ${steps.size}.")
        }
    }

    override fun shutdown() {
        return
    }

    //PLAYER DATABASE IMPL



}