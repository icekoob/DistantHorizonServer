package com.dibujaron.distanthorizon.ship.controller.ai

import com.dibujaron.distanthorizon.script.ScriptReader
import com.dibujaron.distanthorizon.ship.ShipManager
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.controller.PlayerShipController

class FakePlayerAIController(private val scriptReader: ScriptReader) : PlayerShipController(false) {

    /*override fun getType(): ControllerType {
        return ControllerType.ARTIFICIAL
    }*/

    override fun isRequestingUndock(): Boolean {
        return false
    }

    override fun computeNextState(): ShipState {
        if (scriptReader.hasNextAction()) {
            if (scriptReader.nextActionShouldFire()) {
                receiveInputChange(scriptReader.getNextAction())
            }
        } else {
            println("AI ship ${ship.uuid} completed run and will be removed.")
            ShipManager.markForRemove(this.ship)
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

    /*private fun dock() {
        currentScript = null
        ship.attemptDock(2000.0, 2000.0)
        if (ship.isDocked()) {
            val station = ship.dockedToPort!!.station
            println("AI Ship ${ship.uuid} docked at $station")
            /*val newScript = DHServer.getScriptDatabase()
                .selectAvailableScriptToAnywhere(station, DHServer.getCurrentTickInCycle() + 5, Int.MAX_VALUE)
            if (newScript == null) {
                println("AI ship found no script to proceed, will remain docked.")
            } else {
                nextDepartureTick = newScript.getDepartureTick()
            }*/
            ShipManager.markForRemove(this.ship)
        } else {
            throw java.lang.IllegalStateException("AI ship ${ship.uuid} should have docked but failed to dock.")
        }
    }*/
}