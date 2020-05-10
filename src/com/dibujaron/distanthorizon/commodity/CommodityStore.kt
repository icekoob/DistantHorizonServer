package com.dibujaron.distanthorizon.commodity

import org.json.JSONObject
import java.util.*

class CommodityStore(val type: CommodityType, properties: Properties) {

    val identifyingName = type.identifyingName
    val displayName = type.displayName
    val buyPrice: Double = properties.getProperty("$identifyingName.buy", "9999.0").toDouble()
    val sellPrice: Double = properties.getProperty("$identifyingName.sell", "0.0").toDouble()
    var quantityAvailable: Int = properties.getProperty("$identifyingName.initial", "0").toInt()

    fun createStoreJson(): JSONObject {
        val retval = JSONObject()
        retval.put("identifying_name", identifyingName)
        retval.put("display_name", displayName)
        retval.put("buy_price", buyPrice)
        retval.put("sell_price", sellPrice)
        retval.put("quantity_available", quantityAvailable)
        return retval
    }
}