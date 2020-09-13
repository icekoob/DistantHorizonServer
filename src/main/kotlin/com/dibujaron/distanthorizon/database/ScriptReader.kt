package com.dibujaron.distanthorizon.database

import com.dibujaron.distanthorizon.ship.ShipInputs
import com.dibujaron.distanthorizon.ship.ShipState

interface ScriptReader {

    fun copy(): ScriptReader

    fun getDepartureTick(): Int

    fun getStartingState(): ShipState

    fun getMainThrustPower(): Double

    fun getManuThrustPower(): Double

    fun getRotationPower(): Double

    fun hasNextAction(): Boolean

    fun nextActionShouldFire(): Boolean

    fun getNextAction(): ShipInputs
}