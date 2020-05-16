package com.dibujaron.distanthorizon.docking

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.Ship
import org.json.JSONObject

class ShipDockingPort(private val ship: Ship, private val shipClassDockingPort: ShipClassDockingPort) : DockingPort {

    val rotation = Math.toRadians(shipClassDockingPort.rotationDegrees)
    override fun globalPosition(): Vector2 {
        return ship.globalPos + shipClassDockingPort.relativePos
    }

    override fun relativePosition(): Vector2
    {
        return shipClassDockingPort.relativePos
    }

    override fun getVelocity(): Vector2 {
        return ship.velocity
    }

    override fun globalRotation(): Double {
        return ship.rotation + rotation
    }

    override fun relativeRotation(): Double {
        return rotation
    }
}