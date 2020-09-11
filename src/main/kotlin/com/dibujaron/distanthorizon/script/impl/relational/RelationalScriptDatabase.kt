package com.dibujaron.distanthorizon.script.impl.relational

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.script.ScriptDatabase
import com.dibujaron.distanthorizon.script.ScriptReader
import com.dibujaron.distanthorizon.script.ScriptWriter
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

class RelationalScriptDatabase(val databaseUrl: String, val databaseDriver: String) : ScriptDatabase() {

    object Route : IntIdTable() {
        val originStation: Column<String> =
            varchar("OriginStation", 32) //todo make these foreign keys to a stations table?
        val destinationStation: Column<String> = varchar("DestinationStation", 32)
        val departureTick: Column<Int> = integer("DepartureTick")
        val arrivalTick: Column<Int> = integer("ArrivalTick")
        val startingLocationX: Column<Double> = double("StartingLocationX")
        val startingLocationY: Column<Double> = double("StartingLocationY")
        val startingRotation: Column<Double> = double("StartingRotation")
        val startingVelocityX: Column<Double> = double("StartingVelocityX")
        val startingVelocityY: Column<Double> = double("StartingVelocityY")
        val mainThrustPower: Column<Double> = double("MainThrustPower")
        val manuThrustPower: Column<Double> = double("ManuThrustPower")
        val rotationPower: Column<Double> = double("RotationPower")

    }

    object RouteStep : IntIdTable() {
        val routeID = reference("RouteID", Route.id).uniqueIndex() //what does unique index do here?
        val stepTick: Column<Int> = integer("StepTick")
        val mainEngines: Column<Boolean> = bool("MainEngines")
        val tillerLeft: Column<Boolean> = bool("TillerLeft")
        val tillerRight: Column<Boolean> = bool("TillerRight")
        val portThrusters: Column<Boolean> = bool("PortThrusters")
        val stbdThrusters: Column<Boolean> = bool("StbdThrusters")
        val foreThrusters: Column<Boolean> = bool("ForeThrusters")
        val aftThrusters: Column<Boolean> = bool("AftThrusters")
    }

    init {
        Database.connect(databaseUrl, driver = databaseDriver)
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

    //for now, the best script is the one with the earliest arrival date.
    override fun selectAvailableScript(
        sourceStation: Station,
        targetStation: Station,
        earliestDepartureTick: Int,
        latestDepartureTick: Int
    ): ScriptReader? {

        val originStationFilter = (RelationalScriptDatabase.Route.originStation eq sourceStation.name)
        val destStationFilter = (RelationalScriptDatabase.Route.destinationStation eq targetStation.name)
        val stationFilters = originStationFilter and destStationFilter
        val departureTickLowerLimit = (RelationalScriptDatabase.Route.departureTick greater earliestDepartureTick)
        val departureTickUpperLimit = (RelationalScriptDatabase.Route.departureTick less latestDepartureTick)
        val timeFilters = departureTickLowerLimit and departureTickUpperLimit
        val routeFilters = stationFilters and timeFilters

        val route = transaction {
            Route.select { routeFilters }.orderBy(Route.arrivalTick to SortOrder.ASC)
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

    class RelationalScriptReader(private val route: ResultRow) : ScriptReader {

        private val steps: TreeMap<Int, ResultRow> = TreeMap()

        init {
            transaction { RouteStep.select { RouteStep.routeID eq route[Route.id] } }
                .forEach { steps[it[RouteStep.stepTick]] = it }
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
            val currentTick = DHServer.getCurrentTick()
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
        return RelationalScriptWriter(sourceStation, startState, shipClass)
    }

    class RelationalScriptWriter(
        private val sourceStation: Station,
        private val startState: ShipState,
        private val shipClass: ShipClass,
        private val startTick: Int = DHServer.getCurrentTick()
    ) : ScriptWriter {
        private val steps: TreeMap<Int, ShipInputs> = TreeMap()

        override fun writeAction(action: ShipInputs) {
            steps[DHServer.getCurrentTick()] = action
        }

        //todo make not blocking if required
        override fun completeScript() {
            transaction {
                val newRouteId = Route.insertAndGetId {
                    it[originStation] = sourceStation.name
                    it[departureTick] = startTick
                    it[arrivalTick] = DHServer.getCurrentTick()
                    it[startingLocationX] = startState.position.x
                    it[startingLocationY] = startState.position.y
                    it[startingRotation] = startState.rotation
                    it[startingVelocityX] = startState.velocity.x
                    it[startingVelocityY] = startState.velocity.y
                    it[mainThrustPower] = shipClass.mainThrust
                    it[manuThrustPower] = shipClass.manuThrust
                    it[rotationPower] = shipClass.rotationPower
                }

                RouteStep.batchInsert(steps.entries){ entry ->
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
        }
    }

    override fun shutdown() {
        return
    }


}