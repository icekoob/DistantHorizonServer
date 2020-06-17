package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import org.json.JSONObject
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.math.*

abstract class Orbiter(val properties: Properties) {
    val name: String = properties.getProperty("name").trim()
    val parentName: String = properties.getProperty("parent").trim()

    var initialized = false;
    var parent: Planet? = null;

    var orbitalSpeed: Double = 0.0
    var relativePos: Vector2 = Vector2(0, 0)
    var orbitalRadius: Double = 0.0
    var angularVelocity: Double = 0.0

    open fun scale(): Double {
        return 1.0
    }

    //called after all of the orbiters have loaded from file.
    fun initialize() {
        if (!initialized) {
            if (parentName.isEmpty()) {
                println("Initialized orbiter $name as stationary object at position $relativePos.")
                relativePos = loadStartingPositionAndScale(properties, 1.0)
                orbitalSpeed = 0.0
                orbitalRadius = relativePos.length
            } else {
                val foundParent: Planet? = OrbiterManager.getPlanet(parentName)
                if (foundParent == null) {
                    throw IllegalArgumentException("parent planet $parentName not found.")
                } else {
                    foundParent.initialize()
                    relativePos = loadStartingPositionAndScale(properties, foundParent.scale())
                    orbitalRadius = relativePos.length
                    orbitalSpeed = sqrt((OrbiterManager.gravityConstant * foundParent.mass) / orbitalRadius)
                    parent = foundParent;

                }
            }
            if (orbitalRadius > 0) {
                angularVelocity = orbitalSpeed / orbitalRadius
            } else {
                angularVelocity = 0.0
            }
            println("Initialized orbiter $name with parent $parent at relative position $relativePos")
            initialized = true;
        }
    }

    open fun createOrbiterJson(): JSONObject {
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
        return velocityAtTime(0.0)
    }

    fun velocityAtTime(timeOffset: Double): Vector2 {
        return globalPosAtTime(timeOffset + 1) - globalPosAtTime(timeOffset)
    }

    private var currentPosCached = Vector2(0, 0)
    private var currentPosCacheTick = -1
    fun globalPosAtTime(timeOffset: Double): Vector2 {
        return if (timeOffset == 0.0) {
            if (currentPosCacheTick < DHServer.tickCount) {
                currentPosCached = computeGlobalPosAtTime(timeOffset)
                currentPosCacheTick = DHServer.tickCount
            }
            currentPosCached
        } else {
            computeGlobalPosAtTime(timeOffset)
        }
    }

    private fun computeGlobalPosAtTime(timeOffset: Double): Vector2 {
        val parent = this.parent
        return if (parent == null) {
            relativePos
        } else {
            val parentPos = parent.globalPosAtTime(timeOffset)
            parentPos + relativePosAtTime(timeOffset)
        }
    }

    /*private val posCache = TreeMap<Int, Vector2>()
    fun globalPosAtTime(timeOffsetSeconds: Double): Vector2 {
        val parent = this.parent
        return if (parent == null) {
            relativePos
        } else {
            if(posCache.size > 100 && posCache.firstKey() < DHServer.tickCount){
                posCache.subMap(0, DHServer.tickCount).clear()
            }
            val ticksInFuture: Int = (timeOffsetSeconds * DHServer.TICKS_PER_SECOND).roundToInt()
            val key = DHServer.tickCount + ticksInFuture
            posCache.computeIfAbsent(key) {
                val parentPos = parent.globalPosAtTime(timeOffsetSeconds)
                parentPos + relativePosAtTime(timeOffsetSeconds)
            }
        }
    }*/

    fun getStar(): Orbiter {
        val p = parent
        if (p == null) {
            return this
        } else {
            return p.getStar()
        }
    }

    override fun toString(): String {
        return name
    }

    fun relativePosAtTime(timeOffset: Double): Vector2 {
        return if (relativePos.lengthSquared == 0.0) {
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

fun loadStartingPositionAndScale(properties: Properties, parentScale: Double): Vector2 {
    if (properties.containsKey("posX") && properties.containsKey("posY")) {
        val posX = properties.getProperty("posX").toDouble()
        val posY = properties.getProperty("posY").toDouble()
        val retval = Vector2(posX, posY) * parentScale
        return retval;
    } else if (properties.containsKey("orbitalRadius")) {
        val orbitalRadius = properties.getProperty("orbitalRadius").toInt()
        return Vector2(orbitalRadius, 0) * (parentScale)
    } else {
        throw IllegalArgumentException("Properties file must contain posX,posY or orbitalRadius!")
    }
}