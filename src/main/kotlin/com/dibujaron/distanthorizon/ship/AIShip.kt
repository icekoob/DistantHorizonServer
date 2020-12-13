package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.database.script.ScriptReader

class AIShip(private val scriptReader: ScriptReader) : Ship(
    scriptReader.getShipClass(),
    ShipColor.random(),
    ShipColor.random(),
    HashMap(),
    scriptReader.getStartingState(), null) {

    val shipClass = scriptReader.getShipClass()
    override fun computeNextState(): ShipState {
        if (scriptReader.hasNextAction()) {
            if (scriptReader.nextActionShouldFire()) {
                receiveInputChange(scriptReader.getNextAction())
            }
        } else {
            println("AI ship $uuid completed run and will be removed.")
            ShipManager.removeShip(this)
        }
        return super.computeNextState() //will apply the inputs.
    }
}