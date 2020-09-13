package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.docking.DockingPort
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.CommodityType
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.Account
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.database.ScriptWriter
import org.json.JSONObject
import java.util.*
import kotlin.math.pow

open class Ship(
    val type: ShipClass,
    private val primaryColor: ShipColor,
    private val secondaryColor: ShipColor,
    initialState: ShipState,
    val shouldRecordScripts: Boolean
) {

    var currentState: ShipState = initialState
    val uuid = UUID.randomUUID()

    //controls
    val myDockingPorts = type.dockingPorts.asSequence().map { ShipDockingPort(this, it) }.toList()
    var dockedToPort: StationDockingPort? = null
    var myDockedPort: ShipDockingPort? = null

    var holdCapacity = type.holdSize
    var hold = HashMap<String, Int>()

    var scriptWriter: ScriptWriter? = null

    fun holdOccupied(): Int {
        return hold.values.asSequence().sum()
    }

    fun isDocked(): Boolean {
        return dockedToPort != null && myDockedPort != null
    }

    fun createHoldStatusMessage(): JSONObject {
        val retval = JSONObject()
        CommodityType.values().asSequence()
            .map { Pair(it.identifyingName, hold[it.identifyingName] ?: 0) }
            .forEach { retval.put(it.first, it.second) }
        return retval
    }

    var tickCount = 0

    fun tick() {
        val dockedTo = dockedToPort
        val dockedFrom = myDockedPort
        currentState = if (dockedTo != null && dockedFrom != null) {
            val velocity = dockedTo.getVelocity()
            val myPortRelative = dockedFrom.relativePosition()
            val rotation = dockedTo.globalRotation() + dockedFrom.relativeRotation()
            val globalPos = dockedTo.globalPosition() + (myPortRelative * -1.0).rotated(rotation)
            ShipState(globalPos, rotation, velocity)
        } else {
            computeNextState()
        }
        tickCount++
    }

    private var controls: ShipInputs = ShipInputs()
    open fun computeNextState(): ShipState {
        val delta = DHServer.TICK_LENGTH_SECONDS
        var velocity = currentState.velocity
        var globalPos = currentState.position
        var rotation = currentState.rotation
        if (controls.mainEnginesActive) {
            velocity += Vector2(0, -getMainThrust()).rotated(rotation) * delta
        }
        if (controls.stbdThrustersActive) {
            velocity += Vector2(-getManuThrust(), 0).rotated(rotation) * delta
        }
        if (controls.portThrustersActive) {
            velocity += Vector2(getManuThrust(), 0).rotated(rotation) * delta
        }
        if (controls.foreThrustersActive) {
            velocity += Vector2(0, getManuThrust()).rotated(rotation) * delta
        }
        if (controls.aftThrustersActive) {
            velocity += Vector2(0, -getManuThrust()).rotated(rotation) * delta
        }
        if (controls.tillerLeft) {
            rotation -= getRotationPower() * delta
        } else if (controls.tillerRight) {
            rotation += getRotationPower() * delta
        }
        velocity += OrbiterManager.calculateGravityAtTick(0.0, globalPos) * delta
        globalPos += velocity * delta
        return ShipState(globalPos, rotation, velocity)
    }

    open fun getMainThrust(): Double {
        return type.mainThrust
    }

    open fun getManuThrust(): Double {
        return type.manuThrust
    }

    open fun getRotationPower(): Double {
        return type.rotationPower
    }

    fun createFullShipJSON(): JSONObject {
        val retval = createShipHeartbeatJSON()
        retval.put("type", type.qualifiedName)
        retval.put("hold_size", type.holdSize)
        retval.put("main_engine_thrust", getMainThrust())
        retval.put("manu_engine_thrust", getManuThrust())
        retval.put("rotation_power", getRotationPower())
        retval.put("primary_color", primaryColor.toJSON())
        retval.put("secondary_color", secondaryColor.toJSON())
        retval.put("docking_ports", myDockingPorts.asSequence().map { it.toJSON() }.toList())
        retval.put("docked", isDocked())
        if (isDocked()) {
            retval.put("docked_info", createDockedMessage())
        }
        return retval
    }

    fun createShipHeartbeatJSON(): JSONObject {
        val retval = JSONObject()
        retval.put("id", uuid)
        retval.put("velocity", currentState.velocity.toJSON())
        retval.put("global_pos", currentState.position.toJSON())
        retval.put("rotation", currentState.rotation)
        retval.put("hold_occupied", holdOccupied())
        retval.put("main_engines", controls.mainEnginesActive)
        retval.put("port_thrusters", controls.portThrustersActive)
        retval.put("stbd_thrusters", controls.stbdThrustersActive)
        retval.put("fore_thrusters", controls.foreThrustersActive)
        retval.put("aft_thrusters", controls.aftThrustersActive)
        retval.put("rotating_left", controls.tillerLeft)
        retval.put("rotating_right", controls.tillerRight)
        return retval
    }

    fun attemptDock(maxDockDist: Double = DHServer.dockingDist, maxClosingSpeed: Double = DHServer.dockingSpeed) {
        val maxDistSquared = maxDockDist.pow(2)
        val maxClosingSpeedSquared = maxClosingSpeed.pow(2)

        val match = OrbiterManager.getStations().asSequence()
            .flatMap { it.dockingPorts.asSequence() }
            .flatMap { stationPort ->
                myDockingPorts.asSequence()
                    .map { shipPort -> Pair(shipPort, stationPort) }
            }
            .filter { it.first.relativeRotation() + it.second.relativeRotation == 0.0 } //only want docking position facing forward
            .filter { (it.first.getVelocity() - it.second.getVelocity()).lengthSquared < maxClosingSpeedSquared }
            .map { Triple(it.first, it.second, (it.first.globalPosition() - it.second.globalPosition()).lengthSquared) }
            .filter { it.third < maxDistSquared }
            .minByOrNull { it.third }

        if (match != null) {
            val bestShipPort = match.first
            val bestStationPort = match.second
            dock(bestShipPort, bestStationPort)
        } else {
            println("ship $uuid Found no match to dock.")
        }
    }

    private fun dock(shipPort: ShipDockingPort, stationPort: StationDockingPort) {
        this.myDockedPort = shipPort
        this.dockedToPort = stationPort
        DHServer.broadcastShipDocked(this)
        scriptWriter?.completeScript(stationPort.station)
    }

    fun createDockedMessage(): JSONObject {
        val myPort: DockingPort? = this.myDockedPort
        val stationPort: StationDockingPort? = this.dockedToPort;
        if (myPort == null || stationPort == null) {
            throw IllegalStateException("Creating docked message but not docked.")
        } else {
            val dockedMessage = JSONObject()
            dockedMessage.put("id", uuid)
            dockedMessage.put("station_identifying_name", stationPort.station.name)
            dockedMessage.put("ship_port", myPort.toJSON())
            dockedMessage.put("station_port", stationPort.toJSON())
            return dockedMessage
        }
    }

    fun undock() {
        val dockedTo = dockedToPort
        if (dockedTo != null) {
            DHServer.broadcastShipUndocked(this)
            if (shouldRecordScripts) {
                scriptWriter = DHServer.getScriptDatabase().beginLoggingScript(dockedTo.station, currentState, type)
            }
        }
        dockedToPort = null;
        myDockedPort = null;
    }

    fun receiveInputChange(shipInputs: ShipInputs) {
        controls = shipInputs
        scriptWriter?.writeAction(shipInputs)
        broadcastInputsChange()
    }

    private fun broadcastInputsChange() {
        val inputsUpdate = createShipHeartbeatJSON()
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

    fun buyResourceFromStation(commodity: String, purchasingAccount: Account, quantity: Int) {
        if (isDocked()) {
            val station = dockedToPort!!.station
            station.sellResourceToShip(commodity, purchasingAccount, this, quantity)
        }
    }

    fun sellResourceToStation(commodity: String, purchasingAccount: Account, quantity: Int) {
        if (isDocked()) {
            val station = dockedToPort!!.station
            station.buyResourceFromShip(commodity, purchasingAccount, this, quantity)
        }
    }
}
