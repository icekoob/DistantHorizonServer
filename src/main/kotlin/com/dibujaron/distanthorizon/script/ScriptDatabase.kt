package com.dibujaron.distanthorizon.script

import com.dibujaron.distanthorizon.orbiter.Station

abstract class ScriptDatabase {
    abstract fun selectStationsWithScripts(): List<Station>
    abstract fun selectAvailableScript(sourceStation: Station, targetStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    abstract fun selectAvailableScriptToAnywhere(sourceStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    abstract fun beginLoggingScript(sourceStation: Station): ScriptWriter
    abstract fun shutdown()
}