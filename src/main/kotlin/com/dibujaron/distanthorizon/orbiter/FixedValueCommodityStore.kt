package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.utils.TimeUtils
import java.util.*

const val UPDATE_TIME_TICKS = 5 * 60

class FixedValueCommodityStore(type: CommodityType, properties: Properties) : CommodityStore(type) {

    private val initialPrice: Int = properties.getProperty("${type.identifyingName}.price", "0").toIntOrNull() ?: 0
    private val initialQuantity: Int = properties.getProperty("${type.identifyingName}.initial", "0").toInt()

    //this is temporary and not great.
    val productionConsumptionRate =
        if (initialQuantity > 0) (price * 10) else -1 * (price * 10)

    init {
        quantityAvailable = initialQuantity
        price = initialPrice
    }

    var lastUpdateTick = 0
    override fun tick() {
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
}