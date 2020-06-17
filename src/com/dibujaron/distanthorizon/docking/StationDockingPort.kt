package com.dibujaron.distanthorizon.docking

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.Station

class StationDockingPort(val station: Station, val relativePos: Vector2, val rotationDegrees: Double) : DockingPort {
    val relativeRotation = Math.toRadians(rotationDegrees)

    override fun globalPosition(): Vector2 {
        return globalPosAtTime(0.0)
    }

    fun globalPosAtTime(time: Double): Vector2 {
        return station.globalPosAtTime(time) + relativePos.rotated(station.globalRotationAtTime(time))
    }

    override fun getVelocity(): Vector2 {
        return velocityAtTime(0.0)
    }

    fun velocityAtTime(timeOffset: Double): Vector2 {
        return station.velocityAtTime(timeOffset)
    }

    override fun globalRotation(): Double {
        return globalRotationAtTime(0.0)
    }

    fun globalRotationAtTime(timeOffset: Double): Double {
        return station.globalRotationAtTime(timeOffset) + relativeRotation
    }

    override fun relativePosition(): Vector2 {
        return relativePos
    }

    override fun relativeRotation(): Double {
        return relativeRotation
    }
}