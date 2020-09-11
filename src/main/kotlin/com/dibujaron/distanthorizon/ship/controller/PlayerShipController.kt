package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.script.ScriptWriter
import com.dibujaron.distanthorizon.ship.ShipInputs
import com.dibujaron.distanthorizon.ship.ShipState
import org.json.JSONObject

open class PlayerShipController(private val shouldRecordScripts: Boolean) : ShipController() {

    var scriptWriter: ScriptWriter? = null
    override fun getType(): ControllerType {
        return ControllerType.PLAYER
    }

    override fun getHeartbeat(): JSONObject {
        val retval = JSONObject()
        retval.put("main_engines", controls.mainEnginesActive)
        retval.put("port_thrusters", controls.portThrustersActive)
        retval.put("stbd_thrusters", controls.stbdThrustersActive)
        retval.put("fore_thrusters", controls.foreThrustersActive)
        retval.put("aft_thrusters", controls.aftThrustersActive)
        retval.put("rotating_left", controls.tillerLeft)
        retval.put("rotating_right", controls.tillerRight)
        return retval
    }

    var controls: ShipInputs = ShipInputs()

    override fun computeNextState(): ShipState {
        val delta = DHServer.TICK_LENGTH_SECONDS
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

    override fun isRequestingUndock(): Boolean {
        return false;
    }

    fun receiveInputChange(shipInputs: ShipInputs) {
        controls = shipInputs
        scriptWriter?.writeAction(shipInputs)
        broadcastInputsChange()
    }

    fun dockOrUndock() {
        if (ship.isDocked()) {
            val dockedStation = ship.dockedToPort!!.station
            ship.undock()
            if (shouldRecordScripts) {
                scriptWriter =
                    DHServer.getScriptDatabase().beginLoggingScript(dockedStation, ship.currentState, ship.type)
            }
        } else {
            ship.attemptDock()
            if (ship.isDocked()) {
                scriptWriter?.completeScript(ship.dockedToPort!!.station)
            }
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