package com.dibujaron.distanthorizon.orbiter

import org.json.JSONObject
import java.lang.IllegalArgumentException
import java.util.*

class Planet(properties: Properties): Orbiter(properties){
    val type = properties.getProperty("type").toString()
    val mass = loadMass(properties)
    val tidalLock = if(properties.containsKey("tidalLock")) properties.getProperty("tidalLock")!!.toBoolean() else false
    val rotationSpeed: Double = if(properties.containsKey("rotationSpeed")) properties.getProperty("rotationSpeed").toDouble() else 0.0
    val minOrbitalRadius = properties.getProperty("minOrbitalRadius").toInt()
    val minRadiusSquared = minOrbitalRadius * minOrbitalRadius

    override fun scale(): Double{
        val par = parent
        val type = properties.getProperty("type")
        val typeScale = typeScale(properties.getProperty("type").toString())
        return if (properties.containsKey("scale")) {
            typeScale * properties.getProperty("scale").toDouble()
        } else {
            typeScale * (par?.scale() ?: 1.0)
        }
    }

    override fun createOrbiterJson(): JSONObject {
        val retval = super.createOrbiterJson()
        retval.put("tidal_lock", tidalLock)
        retval.put("rotation_speed", rotationSpeed)
        retval.put("scale", scale())
        retval.put("type", type)
        return retval
    }

    /*fun cumulativeScale(): Double {
        val par = parent;
        if(par != null){
            var parScale = par.cumulativeScale()
            var myScale = parScale * scaleProperty;
            return myScale;
        } else {
            return scaleProperty;
        }
    }*/

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

fun typeScale(type: String): Double{
    when(type){
        "Star" -> return 1.0
        "Continental" -> return 0.2
        "Moon" -> return 0.1
        "Gas" -> return 0.75
        else -> return 1.0
    }
}