package com.dibujaron.distanthorizon.ship

import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.collections.HashMap

object ShipClassManager {
    private val shipClassMap: HashMap<String, ShipClass> = HashMap()

    init {
        File("./shipclasses/").walk()
            .filter { it.name.endsWith(".properties") }
            .map { FileReader(it) }
            .map { loadProperties(it) }
            .map { ShipClass(it) }
            .forEach {
                shipClassMap[it.qualifiedName] = it
            }
    }

    fun loadProperties(reader: FileReader): Properties {
        val props = Properties()
        props.load(reader)
        return props
    }

    fun getShipClass(qualName: String): ShipClass?
    {
        return shipClassMap[qualName]
    }

    fun getShipClassRequired(qualName: String): ShipClass
    {
        val res = getShipClass(qualName)
        if(res == null){
            throw IllegalStateException("ship class $qualName not found")
        } else {
            return res
        }
    }

    fun getShipClasses(): Collection<ShipClass>
    {
        return shipClassMap.values
    }
}