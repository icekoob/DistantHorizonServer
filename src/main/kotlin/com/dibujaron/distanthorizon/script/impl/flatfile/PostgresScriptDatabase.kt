package com.dibujaron.distanthorizon.script.impl.flatfile

import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.script.ScriptDatabase
import com.dibujaron.distanthorizon.script.ScriptReader
import com.dibujaron.distanthorizon.script.ScriptWriter
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less


const val DATABASE_URL = "jdbc:localhost"
const val DATABASE_DRIVER = "org.postgresql.Driver"

class PostgresScriptDatabase : ScriptDatabase() {

    object Route : IntIdTable() {
        val originStation: Column<String> =
            varchar("OriginStation", 32) //todo make these foreign keys to a stations table?
        val destinationStation: Column<String> = varchar("DestinationStation", 32)
        val departureTick: Column<Int> = integer("DepartureTick")
        val arrivalTick: Column<Int> = integer("ArrivalTick")
        val startingLocationX: Column<Int> = integer("StartingLocationX")
        val startingLocationY: Column<Int> = integer("StartingLocationY")
        val startingRotation: Column<Int> = integer("StartingRotation")
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
        Database.connect(DATABASE_URL, driver = DATABASE_DRIVER)
    }

    override fun selectStationsWithScripts(): List<Station> {
        return Route.slice(Route.originStation).selectAll()
            .withDistinct()
            .map { it[Route.originStation] }
            .mapNotNull { OrbiterManager.getStation(it) }
            .toList()
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

        val route = Route.select { routeFilters }
                .orderBy(Route.arrivalTick to SortOrder.ASC)
                .limit(1)
                .first()

        return getReaderForRoute(route)
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

        val route = Route.select { routeFilters }
            .orderBy(Route.departureTick to SortOrder.ASC) //leave asap don't care where
            .limit(1)
            .first()

        return getReaderForRoute(route)
    }

    private fun getReaderForRoute(chosenRoute: ResultRow): ScriptReader {
        val routeID = chosenRoute[Route.id]
        val routeSteps = RouteStep.select{RouteStep.routeID eq routeID}
            .orderBy(RouteStep.stepTick to SortOrder.ASC) //should already be sorted?
            .
    }

    override fun beginLoggingScript(sourceStation: Station): ScriptWriter {
        TODO("Not yet implemented")
    }

    override fun shutdown() {
        TODO("Not yet implemented")
    }
}