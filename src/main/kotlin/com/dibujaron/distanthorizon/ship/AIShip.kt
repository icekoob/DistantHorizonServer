package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.script.ScriptReader

class AIShip(private val scriptReader: ScriptReader) : Ship(
    scriptReader.getShipClass(),
    ShipColor.random(),
    ShipColor.random(),
    scriptReader.getStartingState(),
    false
) {

    val shipClass = scriptReader.getShipClass()
    override fun computeNextState(): ShipState {
        if (scriptReader.hasNextAction()) {
            if (scriptReader.nextActionShouldFire()) {
                receiveInputChange(scriptReader.getNextAction())
            }
        } else {
            println("AI ship $uuid completed run and will be removed.")
            ShipManager.markForRemove(this)
        }
        return super.computeNextState() //will apply the inputs.
    }
}