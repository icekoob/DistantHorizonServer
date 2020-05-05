package com.dibujaron.distanthorizon.ship

import java.lang.IllegalArgumentException

enum class ShipModel(
    val make: ShipMake,
    val identifyingName: String,
    val displayName: String,
    val rotationPower: Double,
    val mainThrust: Double,
    val manuThrust: Double
) {
    RIJAY_MOCKINGBIRD(ShipMake.RIJAY, "Mockingbird", 6.0, 120.0, 30.0),
    PHE_THUMPER(ShipMake.PHE, "Thumper", 3.0, 120.0, 30.0);

    constructor(
        make: ShipMake,
        displayName: String,
        rotationPower: Double,
        mainThrust: Double,
        manuThrust: Double
    ) : this(make, displayName.toLowerCase(), displayName, rotationPower, mainThrust, manuThrust)

    fun qualifiedName(): String
    {
        return make.identifyingName + "." + identifyingName;
    }

    companion object {
        fun fromQualifiedName(qualName: String): ShipModel
        {
            val split: List<String> = qualName.split('.')
            val make: ShipMake? = ShipMake.fromIdentifyingName(split[0])
            if(make == null){
                throw IllegalArgumentException("failed to parse qualifiedName $qualName, make ${split[0]} not found.")
            } else {
                val model: ShipModel? = values().asSequence()
                    .filter{it.make == make}
                    .find{it.identifyingName == split[1]}

                if(model == null){
                    throw IllegalArgumentException("failed to parse qualifiedName $qualName, model ${split[1]} not found")
                } else {
                    return model
                }
            }
        }
    }
}