package com.dibujaron.distanthorizon.docking

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.Ship

class ShipDockingPort(val ship: Ship, val relativePos: Vector2): DockingPort {
    override fun globalPosition(): Vector2 {
        return ship.globalPos + relativePos
    }
    override fun getVelocity(): Vector2 {
        return ship.velocity
    }
}