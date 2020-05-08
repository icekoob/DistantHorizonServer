package com.dibujaron.distanthorizon.docking

import com.dibujaron.distanthorizon.Vector2

interface DockingPort {
    fun globalPosition(): Vector2
    fun getVelocity(): Vector2
    fun globalRotation(): Double
    fun relativePosition(): Vector2
    fun relativeRotation(): Double
}