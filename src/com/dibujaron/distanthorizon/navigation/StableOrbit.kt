package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Planet
import kotlin.math.pow
import kotlin.math.sqrt

class StableOrbit(val body: Planet, val speed: Double, val altitude: Double) {

    companion object {
        fun fromAltitude(body: Planet, altitude: Double): StableOrbit {
            val speed: Double = sqrt((OrbiterManager.gravityConstant * body.mass) / altitude)
            return StableOrbit(body, speed, altitude)
        }

        fun fromSpeed(body: Planet, speed: Double): StableOrbit {
            val altitude: Double = OrbiterManager.gravityConstant * body.mass / speed.pow(2.0)
            return StableOrbit(body, speed, altitude)
        }
    }
}