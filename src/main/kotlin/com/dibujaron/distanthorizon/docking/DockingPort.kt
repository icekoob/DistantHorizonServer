package com.dibujaron.distanthorizon.docking

import com.dibujaron.distanthorizon.Vector2
import org.json.JSONObject

interface DockingPort {
    fun globalPosition(): Vector2
    fun getVelocity(): Vector2
    fun globalRotation(): Double
    fun relativePosition(): Vector2
    fun relativeRotation(): Double
    fun toJSON(): JSONObject {
        val retval = JSONObject()
        retval.put("global_rotation", globalRotation())
        retval.put("global_position", globalPosition().toJSON())
        retval.put("relative_rotation", relativeRotation())
        retval.put("relative_position", relativePosition().toJSON())
        return retval
    }
}