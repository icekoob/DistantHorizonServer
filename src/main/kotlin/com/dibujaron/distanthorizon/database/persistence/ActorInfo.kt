package com.dibujaron.distanthorizon.database.persistence

import com.dibujaron.distanthorizon.orbiter.Station
import org.json.JSONObject

open class ActorInfo(
    val displayName: String,
    val balance: Int,
    val lastDockedStation: Station?,
    val ship: ShipInfo
){
    open fun toJSON(): JSONObject {
        val r = JSONObject()
        r.put("display_name", displayName)
        r.put("balance", balance)
        r.put("station_name", lastDockedStation?.name)
        r.put("station_display_name", lastDockedStation?.displayName)
        r.put("ship", ship.toJSON())
        return r
    }
}