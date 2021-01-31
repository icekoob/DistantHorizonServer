package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.utils.TimeUtils
import org.json.JSONObject
import java.util.*

class CommodityStore(val type: CommodityType, properties: Properties) {

    val displayName = type.displayName
    val initialPrice: Int = properties.getProperty("${type.identifyingName}.price", "0").toIntOrNull() ?: 0
    val initialQuantity: Int = properties.getProperty("${type.identifyingName}.initial", "0").toInt()

    val price = initialPrice
    //this is temporary and not great.
    val productionConsumptionRate =
        if (initialQuantity > 0) (price * 10) else -1 * (price * 10)
    var quantityAvailable: Int = initialQuantity

    var lastUpdateTick = 0
    fun tick() {
        val currentTick = TimeUtils.getCurrentTickAbsolute()
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

    fun createStoreJson(): JSONObject {
        val retval = JSONObject()
        retval.put("identifying_name", type.identifyingName)
        retval.put("display_name", displayName)
        retval.put("price", price)
        retval.put("quantity_available", quantityAvailable)
        return retval
    }

    companion object {
        const val UPDATE_TIME_TICKS = 5 * 60
    }
}