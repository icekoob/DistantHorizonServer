package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.Vector2
import org.json.JSONObject

class NavigationState(val position: Vector2, val rotation: Double, val velocity: Vector2) {
    fun toJSON(): JSONObject
    {
        val retval = JSONObject()
        retval.put("global_position", position.toJSON())
        retval.put("rotation", rotation)
        retval.put("velocity", velocity.toJSON())
        return retval
    }
}