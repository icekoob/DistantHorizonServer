package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.DHServer
import org.json.JSONObject
import java.util.*
import kotlin.math.roundToInt

class CommodityStore(val type: CommodityType, properties: Properties) {

    val identifyingName = type.identifyingName
    val displayName = type.displayName
    val price: Double = properties.getProperty("$identifyingName.price", "0.0").toDoubleOrNull() ?: 0.0
    val initialQuantity: Int = properties.getProperty("$identifyingName.initial", "0").toInt()

    //this is temporary and not great.
    val productionConsumptionRate =
        if (initialQuantity > 0) (price * 10).roundToInt() else -1 * (price * 10).roundToInt()
    var quantityAvailable: Int = initialQuantity

    var lastUpdateTick = 0
    fun createStoreJson(): JSONObject {
        val retval = JSONObject()
        retval.put("identifying_name", identifyingName)
        retval.put("display_name", displayName)
        retval.put("price", price)
        retval.put("quantity_available", quantityAvailable)
        return retval
    }

    fun tick() {
        val currentTick = DHServer.getCurrentTick()
        if (currentTick - lastUpdateTick > UPDATE_TIME_TICKS) {
            var newQty = quantityAvailable + productionConsumptionRate
            if (newQty > initialQuantity) {
                newQty = initialQuantity
            } else if (newQty < 0) {
                newQty = 0
            }
            quantityAvailable = newQty
            lastUpdateTick = currentTick
        }
    }

    companion object {
        const val UPDATE_TIME_TICKS = 5 * 60
    }
}