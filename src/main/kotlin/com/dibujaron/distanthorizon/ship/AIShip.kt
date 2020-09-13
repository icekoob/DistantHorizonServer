package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.database.ScriptReader

class AIShip(private val scriptReader: ScriptReader) : Ship(
    ShipClassManager.getShipClasses().random(),
    ShipColor.random(),
    ShipColor.random(),
    scriptReader.getStartingState(),
    false
) {

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

    override fun getMainThrust(): Double {
        return scriptReader.getMainThrustPower()
    }

    override fun getManuThrust(): Double {
        return scriptReader.getManuThrustPower()
    }

    override fun getRotationPower(): Double {
        return scriptReader.getRotationPower()
    }
}