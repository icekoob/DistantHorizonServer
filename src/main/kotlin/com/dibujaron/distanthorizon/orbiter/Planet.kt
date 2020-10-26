package com.dibujaron.distanthorizon.orbiter

import org.json.JSONObject
import java.util.*
import kotlin.math.pow

class Planet(parentName: String?, planetName: String, properties: Properties): Orbiter(parentName, planetName, properties){
    val type = properties.getProperty("type").toString()
    val mass = loadMass(properties)
    val tidalLock = if(properties.containsKey("tidalLock")) properties.getProperty("tidalLock")!!.toBoolean() else false
    val rotationSpeed: Double = if(properties.containsKey("rotationSpeed")) properties.getProperty("rotationSpeed").toDouble() else 0.0
    val minOrbitalRadius = properties.getProperty("minOrbitalRadius").toInt()
    val minRadiusSquared = minOrbitalRadius * minOrbitalRadius

    override fun scale(): Double{
        val par = parent
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
        retval.put("mass", mass)
        retval.put("min_orbital_altitude", minOrbitalRadius)
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
        return massBase * 10.0.pow(massExp);
    } else {
        throw IllegalArgumentException("Planet properties must contain massBase and massExp")
    }
}

fun typeScale(type: String): Double{
    return when(type){
        "StarRedGiant" -> 1.0
        "StarYellow" -> 1.0
        "StarWhiteDwarf" -> 1.0
        "ContinentalForest" -> 0.2
        "ContinentalDesert" -> 0.2
        "ContinentalPlains" -> 0.2
        "ContinentalOcean" -> 0.2
        "ContinentalIce" -> 0.2
        "RockyBarren" -> 0.1
        "RockyIce" -> 0.1
        "RockyVolcanic" -> 0.1
        "GasGiant" -> 0.75
        else -> 1.0
    }
}