package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.ship.Ship

abstract class NavigationPhase(val ship: Ship){
    abstract fun hasNextStep(delta: Double): Boolean
    abstract fun step(delta: Double): NavigationStep
}