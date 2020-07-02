package com.dibujaron.distanthorizon.docking

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.Station

class StationDockingPort(val station: Station, val relativePos: Vector2, val rotationDegrees: Double) : DockingPort {
    val relativeRotation = Math.toRadians(rotationDegrees)

    override fun globalPosition(): Vector2 {
        return globalPosAtTick(0.0)
    }

    /*fun globalPosAtTime(time: Double): Vector2 {
        return station.globalPosAtTime(time) + relativePos.rotated(station.globalRotationAtTime(time))
    }*/

    fun globalPosAtTick(tick: Double): Vector2 {
        return station.globalPosAtTick(tick) + relativePos.rotated(station.globalRotationAtTick(tick))

    }

    override fun getVelocity(): Vector2 {
        return velocityAtTick(0.0)
    }

    /*fun velocityAtTime(timeOffset: Double): Vector2 {
        return station.velocityAtTime(timeOffset)
    }*/

    fun velocityAtTick(tickOffset: Double): Vector2 {
        return station.velocityAtTick(tickOffset)
    }

    override fun globalRotation(): Double {
        return globalRotationAtTime(0.0)
    }

    fun globalRotationAtTime(timeOffset: Double): Double {
        return station.globalRotationAtTime(timeOffset) + relativeRotation
    }

    fun globalRotationAtTick(tickOffset: Double): Double {
        return station.globalRotationAtTick(tickOffset) + relativeRotation
    }

    override fun relativePosition(): Vector2 {
        return relativePos
    }

    override fun relativeRotation(): Double {
        return relativeRotation
    }
}