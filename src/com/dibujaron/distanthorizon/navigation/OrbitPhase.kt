package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.orbiter.Planet
import com.dibujaron.distanthorizon.ship.Ship

class OrbitPhase(var body: Planet, var startAngle: Double, var angularDist: Double, var altitude: Double, ship: Ship): NavigationPhase(ship){

    //a navigation phase that orbits the given body from startAngle at Altitude through angularDist (could be negative)
    //StableOrbit class may help with this.
    
    override fun hasNextStep(delta: Double): Boolean {
        TODO("Not yet implemented")
    }

    override fun step(delta: Double): NavigationStep {
        TODO("Not yet implemented")
    }
}