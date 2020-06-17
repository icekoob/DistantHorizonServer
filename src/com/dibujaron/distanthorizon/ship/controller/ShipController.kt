package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipInputs

abstract class ShipController {
    lateinit var ship: Ship

    fun initForShip(ship: Ship) {
        this.ship = ship
    }

    abstract fun dockedTick(delta: Double, coursePlottingAllowed: Boolean)
    abstract fun computeNextState(delta: Double): ShipState
    abstract fun getCurrentControls(): ShipInputs
    abstract fun navigatingToTarget(): Boolean
    abstract fun getNavTarget(): ShipState
    abstract fun getHoldOccupied(): Int
    abstract fun getDiagnostic(): String;
}