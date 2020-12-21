package com.dibujaron.distanthorizon.database.persistence

import com.dibujaron.distanthorizon.orbiter.CommodityType
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipColor
import org.json.JSONObject

open class ShipInfo(
    val shipClass: ShipClass,
    val primaryColor: ShipColor,
    val secondaryColor: ShipColor,
    val holdMap: MutableMap<CommodityType, Int>
){
    open fun toJSON(): JSONObject {
        val r = JSONObject()
        r.put("ship_class", shipClass.qualifiedName)
        r.put("primary_color", primaryColor.toJSON())
        r.put("secondary_color", secondaryColor.toJSON())
        CommodityType.values().forEach {
            r.put("hold_qty_" + it.identifyingName, holdMap[it])
        }
        return r
    }
}