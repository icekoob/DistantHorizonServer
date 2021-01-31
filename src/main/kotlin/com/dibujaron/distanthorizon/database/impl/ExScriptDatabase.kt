package com.dibujaron.distanthorizon.database.impl

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.database.persistence.ActorInfo
import com.dibujaron.distanthorizon.database.script.ScriptDatabase
import com.dibujaron.distanthorizon.database.script.ScriptReader
import com.dibujaron.distanthorizon.database.script.ScriptWriter
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipClassManager
import com.dibujaron.distanthorizon.ship.ShipInputs
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.utils.TimeUtils
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class ExScriptDatabase : ScriptDatabase {
    override fun selectStationsWithScripts(): List<Station> {
        return transaction {
            ExDatabase.Route.slice(ExDatabase.Route.originStation).selectAll()
                .withDistinct()
                .map { it[ExDatabase.Route.originStation] }
                .mapNotNull { OrbiterManager.getStation(it) }
                .toList()
        }
    }

    override fun selectScriptsForStation(sourceStation: Station): List<ScriptReader> {
        val originStationFilter = (ExDatabase.Route.originStation eq sourceStation.name)
        return transaction {
            ExDatabase.Route.select { originStationFilter }
                .orderBy(ExDatabase.Route.departureTick)
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

        val originStationFilter = (ExDatabase.Route.originStation eq sourceStation.name)
        val destStationFilter = (ExDatabase.Route.destinationStation eq targetStation.name)
        val stationFilters = originStationFilter and destStationFilter
        val departureTickLowerLimit = (ExDatabase.Route.departureTick greater earliestDepartureTick)
        val departureTickUpperLimit = (ExDatabase.Route.departureTick less latestDepartureTick)
        val timeFilters = departureTickLowerLimit and departureTickUpperLimit
        val routeFilters = stationFilters and timeFilters

        val routes = transaction {
            ExDatabase.Route.select { routeFilters }.orderBy(ExDatabase.Route.duration to SortOrder.ASC).toList()
        }

        val selectedRoute = routes.minByOrNull { it[ExDatabase.Route.departureTick] + it[ExDatabase.Route.duration] } //min by arrival time
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
        val originStationFilter = (ExDatabase.Route.originStation eq sourceStation.name)
        val departureTickLowerLimit = (ExDatabase.Route.departureTick greater earliestDepartureTick)
        val departureTickUpperLimit = (ExDatabase.Route.departureTick less latestDepartureTick)
        val timeFilters = departureTickLowerLimit and departureTickUpperLimit
        val routeFilters = originStationFilter and timeFilters

        val route = transaction {
            ExDatabase.Route.select { routeFilters }
                .orderBy(ExDatabase.Route.departureTick to SortOrder.ASC) //leave asap don't care where
                .limit(1)
                .first()
        }

        return RelationalScriptReader(route)
    }

    inner class RelationalScriptReader(private val steps: TreeMap<Int, ResultRow>, private val route: ResultRow) :
        ScriptReader {

        constructor(route: ResultRow) : this(TreeMap(), route)

        init {
            if (steps.isEmpty()) {
                transaction {
                    ExDatabase.RouteStep.select { ExDatabase.RouteStep.routeID eq route[ExDatabase.Route.id].value }
                        .forEach {
                            steps[it[ExDatabase.RouteStep.stepTick]] = it
                        }
                }
            }
        }

        override fun getDuration(): Int {
            return route[ExDatabase.Route.duration]
        }

        override fun copy(): ScriptReader {
            return RelationalScriptReader(steps, route) //this does selects twice but I don't think I care.
        }

        override fun getDepartureTick(): Int {
            return route[ExDatabase.Route.departureTick]
        }

        override fun getDestinationStation(): Station {
            return OrbiterManager.getStationRequired(route[ExDatabase.Route.destinationStation])
        }

        override fun getSourceStation(): Station {
            return OrbiterManager.getStationRequired(route[ExDatabase.Route.originStation])
        }

        override fun getStartingState(): ShipState {
            return ShipState(
                Vector2(route[ExDatabase.Route.startingLocationX], route[ExDatabase.Route.startingLocationY]),
                route[ExDatabase.Route.startingRotation],
                Vector2(route[ExDatabase.Route.startingVelocityX], route[ExDatabase.Route.startingVelocityY]),
            )
        }

        override fun getShipClass(): ShipClass {
            val shipClassName = route[ExDatabase.Route.shipClass]
            return ShipClassManager.getShipClass(shipClassName)
                ?: throw IllegalStateException("No ship class found $shipClassName")
        }

        override fun hasNextAction(): Boolean {
            return steps.isNotEmpty()
        }

        override fun nextActionShouldFire(): Boolean {
            val nextActionTick = steps.firstKey()
            val currentTick = TimeUtils.getCurrentTickInCycle()
            return nextActionTick == currentTick
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
                        step[ExDatabase.RouteStep.mainEngines],
                        step[ExDatabase.RouteStep.portThrusters],
                        step[ExDatabase.RouteStep.stbdThrusters],
                        step[ExDatabase.RouteStep.foreThrusters],
                        step[ExDatabase.RouteStep.aftThrusters],
                        step[ExDatabase.RouteStep.tillerLeft],
                        step[ExDatabase.RouteStep.tillerRight]
                    )
                }
            }
        }
    }

    override fun beginLoggingScript(
        actorInfo: ActorInfo?,
        sourceStation: Station,
        startState: ShipState,
        shipClass: ShipClass
    ): ScriptWriter {
        println("Beginning script logging for ${actorInfo?.displayName}")
        return RelationalScriptWriter(actorInfo, sourceStation, startState, shipClass)
    }

    inner class RelationalScriptWriter(
        private val actor: ActorInfo?,
        private val sourceStation: Station,
        private val startState: ShipState,
        private val shipType: ShipClass,
        private val startTick: Int = TimeUtils.getCurrentTickInCycle(),
        private val startTickAbsolute: Int = TimeUtils.getCurrentTickAbsolute()
    ) : ScriptWriter {
        private val steps: TreeMap<Int, ShipInputs> = TreeMap()

        override fun writeAction(action: ShipInputs) {
            steps[TimeUtils.getCurrentTickInCycle()] = action
        }

        //todo make not blocking if required
        override fun completeScript(dockedStation: Station) {
            println("Saving script...")
            val pilotId = if (actor == null) {
                null
            } else {
                (actor as ExPersistenceDatabase.ActorInfoInternal).id
            }
            transaction {
                val newRouteId = ExDatabase.Route.insertAndGetId {
                    it[originStation] = sourceStation.name
                    it[destinationStation] = dockedStation.name
                    it[departureTick] = startTick
                    it[duration] = TimeUtils.getCurrentTickAbsolute() - startTickAbsolute
                    it[startingLocationX] = startState.position.x
                    it[startingLocationY] = startState.position.y
                    it[startingRotation] = startState.rotation
                    it[startingVelocityX] = startState.velocity.x
                    it[startingVelocityY] = startState.velocity.y
                    it[shipClass] = shipType.qualifiedName
                    it[plottedBy] = pilotId
                }

                ExDatabase.RouteStep.batchInsert(steps.entries) { entry ->
                    val tick = entry.key
                    val inputs = entry.value
                    this[ExDatabase.RouteStep.routeID] = newRouteId
                    this[ExDatabase.RouteStep.stepTick] = tick
                    this[ExDatabase.RouteStep.mainEngines] = inputs.mainEnginesActive
                    this[ExDatabase.RouteStep.tillerLeft] = inputs.tillerLeft
                    this[ExDatabase.RouteStep.tillerRight] = inputs.tillerRight
                    this[ExDatabase.RouteStep.portThrusters] = inputs.portThrustersActive
                    this[ExDatabase.RouteStep.stbdThrusters] = inputs.stbdThrustersActive
                    this[ExDatabase.RouteStep.foreThrusters] = inputs.foreThrustersActive
                    this[ExDatabase.RouteStep.aftThrusters] = inputs.aftThrustersActive
                }
            }
            println("Script saved, step count is ${steps.size}.")
        }
    }

    override fun shutdown() {
        return
    }

}