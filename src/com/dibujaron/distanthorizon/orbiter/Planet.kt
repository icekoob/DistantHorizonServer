package com.dibujaron.distanthorizon.orbiter

import org.json.JSONObject
import java.lang.IllegalArgumentException
import java.util.*

class Planet(properties: Properties): Orbiter(properties){
    val scale = if(properties.containsKey("scale")) properties.getProperty("scale").toDouble() else 1.0
    val type = properties.getProperty("type").toString()
    val mass = loadMass(properties)
    val tidalLock = if(properties.containsKey("tidalLock")) properties.getProperty("tidalLock")!!.toBoolean() else false
    val rotationSpeed: Double = if(properties.containsKey("rotationSpeed")) properties.getProperty("rotationSpeed").toDouble() else 0.0
    val minOrbitalRadius = properties.getProperty("minOrbitalRadius").toInt()
    val minRadiusSquared = minOrbitalRadius * minOrbitalRadius

    override fun toJSON(): JSONObject {
        val retval = super.toJSON()
        retval.put("tidal_lock", tidalLock)
        retval.put("rotation_speed", rotationSpeed)
        retval.put("scale", scale)
        retval.put("type", type)
        return retval
    }
}

fun loadMass(properties: Properties): Double{
    if (properties.containsKey("massBase") && properties.containsKey("massExp")) {
        val massBase = properties.getProperty("massBase").toDouble()
        val massExp = properties.getProperty("massExp").toDouble()
        return massBase * Math.pow(10.0, massExp);
    } else {
        throw IllegalArgumentException("Planet properties must contain massBase and massExp")
    }
}

