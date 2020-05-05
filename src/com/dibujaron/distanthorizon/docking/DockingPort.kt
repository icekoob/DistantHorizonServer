package com.dibujaron.distanthorizon.docking

import com.dibujaron.distanthorizon.Vector2

interface DockingPort {
    fun globalPosition(): Vector2
    fun getVelocity(): Vector2
}