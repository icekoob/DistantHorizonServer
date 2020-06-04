package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.ship.IndexedState
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipInputs

class PlayerShipController() : ShipController {

    lateinit var ship: Ship

    override fun initForShip(ship: Ship) {
        this.ship = ship
    }

    var controls: ShipInputs = ShipInputs()
    override fun next(delta: Double, currentState: ShipState): ShipState {
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
        velocity += OrbiterManager.calculateGravity(0.0, globalPos) * delta
        globalPos += velocity * delta
        return ShipState(globalPos, rotation, velocity)
    }

    override fun getCurrentControls(): ShipInputs {
        return controls
    }

    override fun publishScript(numSteps: Int): Sequence<IndexedState> {
        return emptySequence()
    }

    fun receiveInputChange(shipInputs: ShipInputs) {
        if (controls == shipInputs) {
            return;
        } else {
            controls = shipInputs
            broadcastInputsChange()
        }
    }

    fun dockOrUndock() {
        if (ship.isDocked()) {
            ship.completeUndock()
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