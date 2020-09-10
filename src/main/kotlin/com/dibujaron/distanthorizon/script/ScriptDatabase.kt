package com.dibujaron.distanthorizon.script

import com.dibujaron.distanthorizon.orbiter.Station

interface ScriptDatabase {
    fun selectStationsWithScripts(): List<Station>
    fun selectAvailableScript(sourceStation: Station, targetStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    fun selectAvailableScriptToAnywhere(sourceStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    fun beginLoggingScript(sourceStation: Station): ScriptWriter
    fun shutdown()
}