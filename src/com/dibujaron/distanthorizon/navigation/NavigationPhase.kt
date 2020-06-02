package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.ship.Ship

abstract class NavigationPhase(val startTime: Double, val startState: NavigationState, val ship: Ship){

    open fun computeStates(assumedDelta: Double): Sequence<NavigationState>
    {
        return generateSequence {
            if(hasNextStep(assumedDelta)) step(assumedDelta) else null
        }
    }

    abstract fun hasNextStep(delta: Double): Boolean
    abstract fun step(delta: Double): NavigationState
    abstract fun phaseDuration(assumedDelta: Double): Double
    fun endTime(assumedDelta: Double): Double
    {
        return startTime + phaseDuration(assumedDelta)
    }
}