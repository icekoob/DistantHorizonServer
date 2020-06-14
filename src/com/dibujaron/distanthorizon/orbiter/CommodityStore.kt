package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.DHServer
import org.json.JSONObject
import java.util.*
import kotlin.math.roundToInt

class CommodityStore(val type: CommodityType, properties: Properties) {

    val identifyingName = type.identifyingName
    val displayName = type.displayName
    val buyPrice: Double = properties.getProperty("$identifyingName.buy", "9999.0").toDouble()
    val sellPrice: Double = properties.getProperty("$identifyingName.sell", "0.0").toDouble()
    val initialQuantity: Int = properties.getProperty("$identifyingName.initial", "0").toInt()

    //this is temporary and not great.
    val productionConsumptionRate = if(initialQuantity > 0) (buyPrice * 10).roundToInt() else -1 * (sellPrice * 10).roundToInt()
    var quantityAvailable: Int = initialQuantity

    var lastUpdateTime = 0L
    fun createStoreJson(): JSONObject {
        val retval = JSONObject()
        retval.put("identifying_name", identifyingName)
        retval.put("display_name", displayName)
        retval.put("buy_price", buyPrice)
        retval.put("sell_price", sellPrice)
        retval.put("quantity_available", quantityAvailable)
        return retval
    }

    fun process(delta: Double)
    {
        val now = System.currentTimeMillis()
        if(now - lastUpdateTime > UPDATE_TIME_MILLIS){
            var newQty = quantityAvailable + productionConsumptionRate
            if(newQty > initialQuantity){
                newQty = initialQuantity
            }
            else if(newQty < 0){
                newQty = 0
            }
            quantityAvailable = newQty
            lastUpdateTime = now
        }
    }

    fun buyPrice(): Double
    {
        return buyPrice
    }

    fun sellPrice(): Double
    {
        return sellPrice
    }

    companion object {
        const val UPDATE_TIME_MILLIS = 5 * 1000
    }
}