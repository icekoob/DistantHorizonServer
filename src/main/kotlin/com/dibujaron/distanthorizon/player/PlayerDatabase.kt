package com.dibujaron.distanthorizon.player

import com.dibujaron.distanthorizon.dbimpl.ExposedDatabase
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.script.ScriptReader
import com.dibujaron.distanthorizon.script.ScriptWriter
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipState

interface PlayerDatabase {
    fun selectPlayerByExternalAccountID(): List<ExposedDatabase.PlayerDBO>
    fun selectScriptsForStation(sourceStation: Station): List<ScriptReader>
    fun selectAvailableScript(sourceStation: Station, targetStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    fun selectAvailableScriptToAnywhere(sourceStation: Station, earliestDepartureTick: Int, latestDepartureTick: Int): ScriptReader?
    fun beginLoggingScript(sourceStation: Station, startState: ShipState, shipClass: ShipClass): ScriptWriter
    fun shutdown()
}