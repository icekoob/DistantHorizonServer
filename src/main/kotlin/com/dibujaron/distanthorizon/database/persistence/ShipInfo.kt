package com.dibujaron.distanthorizon.database.persistence

import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipColor
import org.json.JSONObject

open class ShipInfo(
    val shipClass: ShipClass,
    val primaryColor: ShipColor,
    val secondaryColor: ShipColor

){
    open fun toJSON(): JSONObject {
        val r = JSONObject()
        r.put("ship_class", shipClass.qualifiedName)
        r.put("primary_color", primaryColor.toJSON())
        r.put("secondary_color", secondaryColor.toJSON())
        return r
    }
}