package com.dibujaron.distanthorizon.docking

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.navigation.NavigationState
import com.dibujaron.distanthorizon.orbiter.Station

class StationDockingPort(val station: Station, val relativePos: Vector2, val rotationDegrees: Double) : DockingPort {
    val relativeRotation = Math.toRadians(rotationDegrees)

    override fun globalPosition(): Vector2 {
        return station.globalPos() + relativePos.rotated(station.globalRotation())
    }

    fun globalPosAtTime(time: Double): Vector2 {
        return station.globalPosAtTime(time) + relativePos.rotated(station.globalRotationAtTime(time))
    }

    override fun getVelocity(): Vector2 {
        return station.velocity()
    }

    fun velocityAtTime(timeOffset: Double): Vector2 {
        return station.velocityAtTime(timeOffset)
    }

    override fun globalRotation(): Double {
        return station.globalRotation() + relativeRotation;
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