package com.dibujaron.distanthorizon.ship

import org.json.JSONObject
import java.awt.Color

class ShipColor(val baseColor: Color) {

    fun toJSON(): JSONObject {
        val retval = JSONObject()
        retval.put("r", baseColor.red)
        retval.put("g", baseColor.green)
        retval.put("b", baseColor.blue)
        return retval
    }

    companion object {
        fun random(): ShipColor {
            val r = (Math.random() * 256.0).toInt()
            val g = (Math.random() * 256.0).toInt()
            val b = (Math.random() * 256.0).toInt()
            return ShipColor(Color(r, g, b))
        }

        fun fromInt(intVal: Int): ShipColor
        {
            return ShipColor(Color(intVal))
        }
    }

    fun toInt(): Int
    {
        return baseColor.rgb
    }
}