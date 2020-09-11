package com.dibujaron.distanthorizon.ship.controller.ai

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.script.ScriptDatabase
import com.dibujaron.distanthorizon.script.ScriptReader
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.controller.ControllerType
import com.dibujaron.distanthorizon.ship.controller.PlayerShipController

class FakePlayerAIController(scriptDatabase: ScriptDatabase) : PlayerShipController(scriptDatabase,false) {

    override fun getType(): ControllerType {
        return ControllerType.ARTIFICIAL
    }

    var currentScript: ScriptReader? = null
    var nextDepartureTick = Integer.MAX_VALUE

    override fun isRequestingUndock(): Boolean {
        return DHServer.getCurrentTick() > nextDepartureTick
    }

    override fun computeNextState(): ShipState {
        val script = currentScript
        if(script != null){
            if(script.hasNextAction()){
                if(script.nextActionShouldFire()){
                    receiveInputChange(script.getNextAction())
                }
            } else {
                dock()
            }
        }
        return super.computeNextState() //will apply the inputs.
    }

    private fun dock(){
        currentScript = null
        ship.attemptDock(2000.0, 2000.0)
        if (ship.isDocked()) {
            val station = ship.dockedToPort!!.station
            println("AI Ship ${ship.uuid} docked at $station")
            val newScript = scriptDatabase.selectAvailableScriptToAnywhere(station, DHServer.getCurrentTick() + 5, DHServer.getMaxTick())
            if(newScript == null){
                println("AI ship found no script to proceed, will remain docked.")
            } else {
                nextDepartureTick = newScript.getDepartureTick()
            }
            currentScript = newScript
        } else {
            throw java.lang.IllegalStateException("AI ship ${ship.uuid} should have docked but failed to dock.")
        }
    }
}