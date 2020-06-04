package com.dibujaron.distanthorizon.intelligence

import com.dibujaron.distanthorizon.ship.ShipState
import org.json.JSONObject

interface IntelligentController {
    fun stepsToJSON(numToWrite: Int): JSONObject
    fun hasNext(): Boolean //if there's no next then we should be docked.
    fun next(): ShipState
}