package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState
import org.json.JSONObject

abstract class ShipController {
    lateinit var ship: Ship

    fun initForShip(ship: Ship) {
        this.ship = ship
    }

    abstract fun isRequestingUndock(): Boolean
    abstract fun computeNextState(): ShipState
    abstract fun getType(): ControllerType
    abstract fun getHeartbeat(): JSONObject

    open fun getMainThrust(): Double
    {
        return ship.type.mainThrust
    }

    open fun getManuThrust(): Double
    {
        return ship.type.manuThrust
    }

    open fun getRotationPower(): Double
    {
        return ship.type.rotationPower
    }
}