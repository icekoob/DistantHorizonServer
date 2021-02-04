package com.dibujaron.distanthorizon.orbiter

import org.json.JSONObject

abstract class CommodityStore(val type: CommodityType) {
    val displayName = type.displayName
    var quantityAvailable: Int = 0
    var price: Int = 0

    abstract fun tick()

    fun createStoreJson(): JSONObject {
        val retval = JSONObject()
        retval.put("identifying_name", type.identifyingName)
        retval.put("display_name", displayName)
        retval.put("price", price)
        retval.put("quantity_available", quantityAvailable)
        return retval
    }
}