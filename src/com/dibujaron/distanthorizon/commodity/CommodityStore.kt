package com.dibujaron.distanthorizon.commodity

import java.util.*

class CommodityStore(val identifyingName: String, properties: Properties) {

    val buyPrice: Double = properties.getProperty("$identifyingName.buy", "9999.0").toDouble()
    val sellPrice: Double = properties.getProperty("$identifyingName.sell", "0.0").toDouble()
    var quantityAvailable: Int = properties.getProperty("$identifyingName.quantityAvailable", "0").toInt()
}