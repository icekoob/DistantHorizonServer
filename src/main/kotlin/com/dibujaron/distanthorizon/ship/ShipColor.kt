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

        fun fromInt(intVal: Int): ShipColor {
            return ShipColor(Color(intVal))
        }

        fun fromHexString(hex: String): ShipColor {
            return ShipColor(Color.decode(hex.trim()))
        }

        fun fromJSON(json: JSONObject): ShipColor {
            val red = json.getInt("r")
            val green = json.getInt("g")
            val blue = json.getInt("b")
            return ShipColor(Color(red, green, blue))
        }
    }

    fun toInt(): Int {
        return baseColor.rgb
    }
}