package com.dibujaron.distanthorizon.database

import com.dibujaron.distanthorizon.database.persistence.PersistenceDatabase
import com.dibujaron.distanthorizon.database.script.ScriptDatabase

interface DhDatabase {
    fun getScriptDatabase(): ScriptDatabase
    fun getPersistenceDatabase(): PersistenceDatabase
}