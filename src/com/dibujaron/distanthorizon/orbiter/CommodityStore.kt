package com.dibujaron.distanthorizon.orbiter

import org.json.JSONObject
import java.util.*

class CommodityStore(val type: CommodityType, properties: Properties) {

    val identifyingName = type.identifyingName
    val displayName = type.displayName
    val buyPriceInitial: Double = properties.getProperty("$identifyingName.buy", "9999.0").toDouble()
    val sellPriceInitial: Double = properties.getProperty("$identifyingName.sell", "0.0").toDouble()
    val productionRate: Int = properties.getProperty("$identifyingName.productionRate", "0").toInt()

    var buyPriceCurrent = buyPriceInitial
    var sellPriceCurrent = sellPriceInitial
    var quantityAvailable = productionRate

    fun createStoreJson(): JSONObject {
        val retval = JSONObject()
        retval.put("identifying_name", identifyingName)
        retval.put("display_name", displayName)
        retval.put("buy_price", buyPriceCurrent)
        retval.put("sell_price", sellPriceCurrent)
        retval.put("quantity_available", quantityAvailable)
        return retval
    }

    var cumulativeDeltaSinceLastUpdate = 0.0
    fun process(delta: Double)
    {
        cumulativeDeltaSinceLastUpdate += delta
        if(cumulativeDeltaSinceLastUpdate > UPDATE_TIME_SECONDS)
        {
            update()
        }
    }

    var availableLastUpdate = quantityAvailable

    fun update()
    {
        val quantityGained = quantityAvailable - availableLastUpdate
        val productionRate = productionRate
    }

    fun buyPrice(): Double
    {
        return 0.0
    }

    fun sellPrice(): Double
    {
        return 0.0
    }

    fun quantityAvailable(): Double
    {
        return 0.0
    }

    companion object {
        const val UPDATE_TIME_SECONDS = 60
    }
}