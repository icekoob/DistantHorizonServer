package com.dibujaron.distanthorizon.script

import com.dibujaron.distanthorizon.ship.ShipInputs

interface ScriptReader {

    fun getDepartureTick(): Int

    fun hasNextAction(): Boolean
    {
        return false
    }

    fun nextActionReady(): Boolean
    {
        return false
    }

    fun nextAction(): ShipInputs
    {
        return ShipInputs()
    }
}