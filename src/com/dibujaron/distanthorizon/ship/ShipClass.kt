package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.docking.ShipClassDockingPort
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import java.util.*

class ShipClass(
    properties: Properties

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
    val dockingPortCount: Int = properties.getProperty("dockingPortCount").toInt()
    val dockingPorts = generateSequence(0) { it + 1 }
        .take(dockingPortCount)
        .map {
            Pair(
                Vector2(
                    properties.getProperty("dockingPort.$it.relativePosX").toDouble(),
                    properties.getProperty("dockingPort.$it.relativePosY").toDouble()
                ), properties.getProperty("dockingPort.$it.rotationDegrees").toDouble()
            )
        }
        .map { ShipClassDockingPort(it.first, it.second) }.toList()

    init {
        println("Initialized ship class $qualifiedName with ${dockingPorts.size} docking ports.")
    }
}