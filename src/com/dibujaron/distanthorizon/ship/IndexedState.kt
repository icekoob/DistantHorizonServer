package com.dibujaron.distanthorizon.ship

import org.json.JSONObject

class IndexedState(val index: Int, val state: ShipState){
    fun toJSON(): JSONObject
    {
        val retval = JSONObject()
        retval.put("index", index)
        retval.put("state", state.toJSON())
        return retval
    }
}