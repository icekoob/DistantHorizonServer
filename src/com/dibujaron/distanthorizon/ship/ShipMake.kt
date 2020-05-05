package com.dibujaron.distanthorizon.ship

enum class ShipMake(val identifyingName: String, val displayName: String, val abbreviation: String){
    RIJAY("rijay", "Rijay Drive Yards", "Rijay"),
    PHE("phe", "Porter Heavy Engineering", "P.H.E.");

    companion object {
        fun fromIdentifyingName(identifyingName: String): ShipMake?
        {
            return values()
                .asSequence()
                .find{it.identifyingName == identifyingName}
        }
    }
}

