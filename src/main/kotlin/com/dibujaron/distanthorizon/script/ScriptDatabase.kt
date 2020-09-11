package com.dibujaron.distanthorizon.script

import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipState

abstract class ScriptDatabase {
    abstract fun selectStationsWithScripts(): List<Station>
    abstract fun selectAvailableScript(sourceStation: Station, targetStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    abstract fun selectAvailableScriptToAnywhere(sourceStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    abstract fun beginLoggingScript(sourceStation: Station, startState: ShipState, shipClass: ShipClass): ScriptWriter
    abstract fun shutdown()
}