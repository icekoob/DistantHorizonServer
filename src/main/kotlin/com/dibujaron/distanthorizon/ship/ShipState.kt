package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import org.json.JSONObject

class ShipState(val position: Vector2, val rotation: Double, val velocity: Vector2) {

    val velocityTicks: Vector2 by lazy {velocity / DHServer.TICKS_PER_SECOND}
    fun toJSON(): JSONObject
    {
        val retval = JSONObject()
        retval.put("global_position", position.toJSON())
        retval.put("global_rotation", rotation)
        retval.put("velocity", velocity.toJSON())
        return retval
    }

    override fun toString(): String {
        return toJSON().toString()
    }
}