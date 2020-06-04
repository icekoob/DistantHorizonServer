package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState

abstract class NavigationPhase(val startTime: Double, val startState: ShipState, val ship: Ship){

    open fun computeStates(assumedDelta: Double): Sequence<ShipState>
    {
        return generateSequence {
            if(hasNextStep(assumedDelta)) step(assumedDelta) else null
        }
    }

    abstract fun hasNextStep(delta: Double): Boolean
    abstract fun step(delta: Double): ShipState
    abstract fun phaseDuration(assumedDelta: Double): Double
    fun endTime(assumedDelta: Double): Double
    {
        return startTime + phaseDuration(assumedDelta)
    }
}