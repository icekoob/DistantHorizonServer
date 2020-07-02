package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.ship.IndexedState
import com.dibujaron.distanthorizon.ship.ShipInputs
import java.lang.IllegalStateException

class PlayerShipController : ShipController() {

    var controls: ShipInputs = ShipInputs()
    override fun computeNextState(delta: Double): ShipState {
        val currentState = ship.currentState
        var velocity = currentState.velocity
        var globalPos = currentState.position
        var rotation = currentState.rotation
        val type = ship.type
        if (controls.mainEnginesActive) {
            velocity += Vector2(0, -type.mainThrust).rotated(rotation) * delta
        }
        if (controls.stbdThrustersActive) {
            velocity += Vector2(-type.manuThrust, 0).rotated(rotation) * delta
        }
        if (controls.portThrustersActive) {
            velocity += Vector2(type.manuThrust, 0).rotated(rotation) * delta
        }
        if (controls.foreThrustersActive) {
            velocity += Vector2(0, type.manuThrust).rotated(rotation) * delta
        }
        if (controls.aftThrustersActive) {
            velocity += Vector2(0, -type.manuThrust).rotated(rotation) * delta
        }
        if (controls.tillerLeft) {
            rotation -= type.rotationPower * delta
        } else if (controls.tillerRight) {
            rotation += type.rotationPower * delta
        }
        velocity += OrbiterManager.calculateGravityAtTick(0.0, globalPos) * delta
        globalPos += velocity * delta
        return ShipState(globalPos, rotation, velocity)
    }

    override fun getHoldOccupied(): Int {
        return ship.holdOccupied()
    }
    override fun getCurrentControls(): ShipInputs {
        return controls
    }

    override fun getDiagnostic(): String {
        return ""
    }

    override fun navigatingToTarget(): Boolean {
        return false
    }

    override fun getNavTarget(): ShipState {
        throw IllegalStateException("getNavTarget should never be called on player ship controller")
    }

    override fun dockedTick(delta: Double, coursePlottingAllowed: Boolean) {
        //player ship doesn't care if it's docked.
    }

    fun receiveInputChange(shipInputs: ShipInputs) {
        controls = shipInputs
        broadcastInputsChange()
    }

    fun dockOrUndock() {
        if (ship.isDocked()) {
            ship.undock()
        } else {
            ship.attemptDock()
        }
    }

    private fun broadcastInputsChange() {
        val inputsUpdate = ship.createShipHeartbeatJSON()
        inputsUpdate.put("main_engines", controls.mainEnginesActive)
        inputsUpdate.put("port_thrusters", controls.portThrustersActive)
        inputsUpdate.put("stbd_thrusters", controls.stbdThrustersActive)
        inputsUpdate.put("fore_thrusters", controls.foreThrustersActive)
        inputsUpdate.put("aft_thrusters", controls.aftThrustersActive)
        inputsUpdate.put("rotating_left", controls.tillerLeft)
        inputsUpdate.put("rotating_right", controls.tillerRight)
        PlayerManager.getPlayers().asSequence()
            .forEach { it.sendShipInputsUpdate(inputsUpdate) }
    }
}