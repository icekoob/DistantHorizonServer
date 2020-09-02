package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.Ship
import org.json.JSONObject

abstract class ShipController {
    lateinit var ship: Ship

    fun initForShip(ship: Ship) {
        this.ship = ship
    }

    abstract fun undockRequested(delta: Double): Boolean
    abstract fun computeNextState(delta: Double): ShipState
    abstract fun getType(): ControllerType
    abstract fun getHeartbeat(): JSONObject
}