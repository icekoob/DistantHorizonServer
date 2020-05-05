package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.Vector2
import org.json.JSONObject
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

abstract class Orbiter(properties: Properties) {
    val name: String = properties.getProperty("name").trim()
    val parentName: String = properties.getProperty("parent").trim()
    val orbitalSpeed: Int = properties.getProperty("orbitalSpeed").toInt()

    var initialized = false;
    var parent: Planet? = null;

    var relativePos: Vector2 = loadStartingPosition(properties)
    val orbitalRadius = relativePos.length
    val angularVelocity = if(orbitalRadius == 0.0) orbitalRadius else orbitalSpeed / orbitalRadius

    //called after all of the orbiters have loaded from file.
    fun initialize() {
        if (!initialized) {
            if (parentName.isEmpty()) {
                println("Initialized orbiter $name as stationary object at position $relativePos.")
            } else {
                val foundParent: Planet? = OrbiterManager.getPlanet(parentName)
                if (foundParent == null) {
                    throw IllegalArgumentException("parent planet $parentName not found.")
                } else {
                    foundParent.initialize()
                    println("Initialized orbiter $name with parent $foundParent at relative position $relativePos")
                    parent = foundParent;
                }
            }
            initialized = true;
        }
    }

    open fun toJSON(): JSONObject {
        val retval = JSONObject()
        retval.put("name", name)
        retval.put("relative_pos", relativePos.toJSON())
        retval.put("orbital_radius", orbitalRadius)
        retval.put("angular_velocity", angularVelocity)
        retval.put("angular_pos", relativePos.angle)
        retval.put("parent", parentName)
        return retval
    }

    open fun process(delta: Double) {
        relativePos = relativePosAtTime(delta) //this is tricky but correct.
    }

    fun globalPos(): Vector2 {
        return globalPosAtTime(0.0)
    }

    fun velocity(): Vector2 {
        return globalPosAtTime(1.0) - globalPos()
    }

    fun globalPosAtTime(timeOffset: Double): Vector2 {
        val parent = this.parent
        return if (parent == null) {
            relativePos
        } else {
            val parentPos = parent.globalPosAtTime(timeOffset)
            parentPos + relativePosAtTime(timeOffset)
        }
    }

    override fun toString(): String {
        return name
    }

    fun relativePosAtTime(timeOffset: Double): Vector2 {
        return if (relativePos.lengthSquared == 0.0 || timeOffset == 0.0) {
            relativePos
        } else {
            val angleFromParent: Double = relativePos.angle
            val angleOffset: Double = angularVelocity * timeOffset
            val newAngle = angleFromParent + angleOffset
            val newAngleVector = Vector2(cos(newAngle), sin(newAngle))
            newAngleVector * orbitalRadius
        }
    }
}

fun loadStartingPosition(properties: Properties): Vector2 {
    if (properties.containsKey("posX") && properties.containsKey("posY")) {
        val posX = properties.getProperty("posX").toDouble()
        val posY = properties.getProperty("posY").toDouble()
        return Vector2(posX, posY);
    } else if (properties.containsKey("orbitalRadius")) {
        val orbitalRadius = properties.getProperty("orbitalRadius").toInt()
        return Vector2(orbitalRadius, 0);
    } else {
        throw IllegalArgumentException("Properties file must contain posX,posY or orbitalRadius!")
    }
}