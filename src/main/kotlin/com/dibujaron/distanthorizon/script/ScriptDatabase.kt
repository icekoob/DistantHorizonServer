package com.dibujaron.distanthorizon.script

import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipState

interface ScriptDatabase {
    fun selectStationsWithScripts(): List<Station>
    fun selectScriptsForStation(sourceStation: Station): List<ScriptReader>
    fun selectAvailableScript(sourceStation: Station, targetStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    fun selectAvailableScriptToAnywhere(sourceStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    fun beginLoggingScript(sourceStation: Station, startState: ShipState, shipClass: ShipClass): ScriptWriter
    fun shutdown()
}