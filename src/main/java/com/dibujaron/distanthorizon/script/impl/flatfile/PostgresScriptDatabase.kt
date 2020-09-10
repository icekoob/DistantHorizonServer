package com.dibujaron.distanthorizon.script.impl.flatfile

import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.script.ScriptDatabase
import com.dibujaron.distanthorizon.script.ScriptReader
import com.dibujaron.distanthorizon.script.ScriptWriter

class PostgresScriptDatabase : ScriptDatabase {

    constructor(){
        
    }

    override fun selectStationsWithScripts(): List<Station> {
        TODO("Not yet implemented")
    }

    override fun selectAvailableScript(
        sourceStation: Station,
        targetStation: Station,
        earliestDepartureTick: Int,
        latestDepartureTick: Int
    ): ScriptReader? {
        TODO("Not yet implemented")
    }

    override fun selectAvailableScriptToAnywhere(
        sourceStation: Station,
        earliestDepartureTick: Int,
        latestDepartureTick: Int
    ): ScriptReader? {
        TODO("Not yet implemented")
    }

    override fun beginLoggingScript(sourceStation: Station): ScriptWriter {
        TODO("Not yet implemented")
    }

    override fun shutdown() {
        TODO("Not yet implemented")
    }
}