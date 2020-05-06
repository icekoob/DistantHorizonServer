package com.dibujaron.distanthorizon.ship

import java.lang.IllegalArgumentException
import java.util.*

class ShipClass(properties: Properties

) {
    //RIJAY_MOCKINGBIRD(ShipMake.RIJAY, "Mockingbird", 6.0, 120.0, 30.0),
    //PHE_THUMPER(ShipMake.PHE, "Thumper", 3.0, 120.0, 30.0);

    //	"phe.thumper"
    //	"rijay.mockingbird"
    val identifyingName: String = properties.getProperty("identifyingName").trim()
    val displayName: String = properties.getProperty("displayName").trim()
    val manufacturer: String = properties.getProperty("manufacturer").trim()
    val qualifiedName = "$manufacturer.$identifyingName"
    val rotationPower: Double = properties.getProperty("rotationPower").toDouble()
    val mainThrust: Double = properties.getProperty("mainThrust").toDouble()
    val manuThrust: Double = properties.getProperty("manuThrust").toDouble()

    init {
        println("Initialized ship class $qualifiedName")
    }
}