package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.CommodityType
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.PlayerManager
import org.json.JSONObject
import java.util.*
import kotlin.math.pow

class Ship(
    val type: ShipClass,
    var globalPos: Vector2,
    var rotation: Double
) {
    var velocity: Vector2 = Vector2(0, 0)
    val uuid = UUID.randomUUID()

    //controls
    var controls: ShipInputs = ShipInputs()

    val myDockingPorts = type.dockingPorts.asSequence().map { ShipDockingPort(this, it) }.toList()
    var dockedToPort: StationDockingPort? = null
    var myDockedPort: ShipDockingPort? = null

    var holdCapacity = type.holdSize
    var hold = HashMap<String, Int>()

    var manuallyControlled = true

    fun holdOccupied(): Int {
        return hold.values.asSequence().sum()
    }

    fun createHoldStatusMessage(): JSONObject {
        val retval = JSONObject()
        CommodityType.values().asSequence()
            .map { Pair(it.identifyingName, hold[it.identifyingName] ?: 0) }
            .forEach { retval.put(it.first, it.second) }
        return retval
    }

    var tickCount = 0
    fun process(delta: Double) {
        val dockedTo = dockedToPort
        val dockedFrom = myDockedPort
        if (dockedTo != null && dockedFrom != null) {
            velocity = dockedTo.getVelocity()
            val myPortRelative = dockedFrom.relativePosition()
            rotation = dockedTo.globalRotation() + dockedFrom.relativeRotation()
            globalPos = dockedTo.globalPosition() + (myPortRelative * -1.0).rotated(rotation)
        } else {
            if(manuallyControlled) {
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
            } else {
                //todo ai navigation; take the next NavigationStep and apply it.
            }
        }
        tickCount++
    }

    fun createFullShipJSON(): JSONObject {
        val retval = JSONObject()
        retval.put("id", uuid)
        retval.put("type", type.qualifiedName)
        retval.put("velocity", velocity.toJSON())
        retval.put("global_pos", globalPos.toJSON())
        retval.put("rotation", rotation)
        retval.put("main_engines", controls.mainEnginesActive)
        retval.put("port_thrusters", controls.portThrustersActive)
        retval.put("stbd_thrusters", controls.stbdThrustersActive)
        retval.put("fore_thrusters", controls.foreThrustersActive)
        retval.put("aft_thrusters", controls.aftThrustersActive)
        retval.put("rotating_left", controls.tillerLeft)
        retval.put("rotating_right", controls.tillerRight)
        retval.put("main_engine_thrust", type.mainThrust)
        retval.put("manu_engine_thrust", type.manuThrust)
        retval.put("rotation_power", type.rotationPower)
        retval.put("docking_ports", myDockingPorts.asSequence().map{it.toJSON()}.toList())
        return retval
    }

    fun createShipHeartbeatJSON(): JSONObject {
        val retval = JSONObject()
        retval.put("id", uuid)
        retval.put("velocity", velocity.toJSON())
        retval.put("global_pos", globalPos.toJSON())
        retval.put("rotation", rotation)
        retval.put("manually_controlled", manuallyControlled)
        //todo if not manually controlled send navigation steps
        return retval
    }

    fun receiveInputChange(shipInputs: ShipInputs) {
        if(controls == shipInputs){
            return;
        } else {
            controls = shipInputs
            broadcastInputsChange()
        }
    }

    private fun broadcastInputsChange(){
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

    fun dockOrUndock() {
        if (dockedToPort == null) {
            attemptDock()
        } else {
            if(dockedToPort != null){
                DHServer.broadcastShipUndocked(this)
            }
            dockedToPort = null;
            myDockedPort = null;
        }
    }

    fun attemptDock() {
        println("attempting to dock.");
        val maxDockDist = 100.0//10000.0//50.0
        val maxDistSquared = maxDockDist.pow(2)

        val maxClosingSpeed = 1000.0//5000.0//500.0
        val maxClosingSpeedSquared = maxClosingSpeed.pow(2)

        val match = OrbiterManager.getStations().asSequence()
            .flatMap { it.dockingPorts.asSequence() }
            .flatMap { stationPort ->
                myDockingPorts.asSequence()
                    .map { shipPort -> Pair(shipPort, stationPort) }
            }
            .filter { (it.first.getVelocity() - it.second.getVelocity()).lengthSquared < maxClosingSpeedSquared }
            .map { Triple(it.first, it.second, (it.first.globalPosition() - it.second.globalPosition()).lengthSquared) }
            .filter { it.third < maxDistSquared }
            .minBy { it.third }

        if (match != null) {
            val bestShipPort = match.first
            val bestStationPort = match.second
            this.myDockedPort = bestShipPort
            this.dockedToPort = bestStationPort
            println("docked to ${bestStationPort.station.name}");
            DHServer.broadcastShipDocked(this, bestShipPort, bestStationPort.station, bestStationPort);
        }
    }
}