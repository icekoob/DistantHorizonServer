package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.ship.IndexedState
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipInputs

interface ShipController {
    fun initForShip(ship: Ship)
    fun next(delta: Double, currentState: ShipState): ShipState
    fun getCurrentControls(): ShipInputs
    fun publishScript(numSteps: Int): Sequence<IndexedState>
}