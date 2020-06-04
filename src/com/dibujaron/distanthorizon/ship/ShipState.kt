package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.Vector2
import org.json.JSONObject

class ShipState(val position: Vector2, val rotation: Double, val velocity: Vector2) {
    fun toJSON(): JSONObject
    {
        val retval = JSONObject()
        retval.put("global_position", position.toJSON())
        retval.put("rotation", rotation)
        retval.put("velocity", velocity.toJSON())
        return retval
    }
}