package com.dibujaron.distanthorizon.database.script

import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipInputs
import com.dibujaron.distanthorizon.ship.ShipState

interface ScriptReader {

    fun copy(): ScriptReader

    fun getDepartureTick(): Int

    fun getSourceStation(): Station

    fun getDestinationStation(): Station

    fun getStartingState(): ShipState

    fun getShipClass(): ShipClass

    fun hasNextAction(): Boolean

    fun nextActionShouldFire(): Boolean

    fun getNextAction(): ShipInputs
}