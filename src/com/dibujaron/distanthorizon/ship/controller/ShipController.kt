package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.ship.IndexedState
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipInputs

abstract class ShipController {
    lateinit var ship: Ship

    fun initForShip(ship: Ship) {
        this.ship = ship
    }

    abstract fun dockedTick(delta: Double)
    abstract fun computeNextState(delta: Double): ShipState
    abstract fun getCurrentControls(): ShipInputs
    abstract fun publishScript(numSteps: Int): Sequence<IndexedState>
    abstract fun getCurrentStep(): Int
}