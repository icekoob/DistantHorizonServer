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
    var mainEnginesActive: Boolean = false
    var portThrustersActive: Boolean = false
    var stbdThrustersActive: Boolean = false
    var foreThrustersActive: Boolean = false
    var aftThrustersActive: Boolean = false
    var tillerLeft: Boolean = false
    var tillerRight: Boolean = false

    val myDockingPorts = type.dockingPorts.asSequence().map { ShipDockingPort(this, it) }.toList()
    var dockedToPort: StationDockingPort? = null
    var myDockedPort: ShipDockingPort? = null

    var holdCapacity = type.holdSize
    var hold = HashMap<String, Int>()

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
            if (mainEnginesActive) {
                velocity += Vector2(0, -type.mainThrust).rotated(rotation) * delta
            }
            if (stbdThrustersActive) {
                velocity += Vector2(-type.manuThrust, 0).rotated(rotation) * delta
            }
            if (portThrustersActive) {
                velocity += Vector2(type.manuThrust, 0).rotated(rotation) * delta
            }
            if (foreThrustersActive) {
                velocity += Vector2(0, type.manuThrust).rotated(rotation) * delta
            }
            if (aftThrustersActive) {
                velocity += Vector2(0, -type.manuThrust).rotated(rotation) * delta
            }
            if (tillerLeft) {
                rotation -= type.rotationPower * delta
            } else if (tillerRight) {
                rotation += type.rotationPower * delta
            }
            velocity += OrbiterManager.calculateGravity(0.0, globalPos) * delta * 50.0
            globalPos += velocity * delta
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
        retval.put("main_engines", mainEnginesActive)
        retval.put("port_thrusters", portThrustersActive)
        retval.put("stbd_thrusters", stbdThrustersActive)
        retval.put("fore_thrusters", foreThrustersActive)
        retval.put("aft_thrusters", aftThrustersActive)
        retval.put("rotating_left", tillerLeft)
        retval.put("rotating_right", tillerRight)
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
        return retval
    }

    fun receiveInputsAndBroadcast(message: JSONObject) {
        mainEnginesActive = message.getBoolean("main_engines_pressed");
        portThrustersActive = message.getBoolean("port_thrusters_pressed")
        stbdThrustersActive = message.getBoolean("stbd_thrusters_pressed")
        foreThrustersActive = message.getBoolean("fore_thrusters_pressed")
        aftThrustersActive = message.getBoolean("aft_thrusters_pressed")
        //val leftPressed = message.getBoolean("rotate_left_pressed")
        //if(leftPressed && !tillerLeft){
        //    leftPressTime = System.currentTimeMillis()
        //    leftPressStartTick = tickCount
        //} else if(!leftPressed && tillerLeft){
        //    println("left pressed for ${System.currentTimeMillis() - leftPressTime}ms, ${tickCount - leftPressStartTick} ticks. Rotation is $rotation")
        //}
        //tillerLeft = leftPressed

        val rightPressed = message.getBoolean("rotate_right_pressed")
        tillerRight = rightPressed
        broadcastInputsChange()
    }

    fun broadcastInputsChange(){
        val inputsUpdate = createShipHeartbeatJSON()
        inputsUpdate.put("main_engines", mainEnginesActive)
        inputsUpdate.put("port_thrusters", portThrustersActive)
        inputsUpdate.put("stbd_thrusters", stbdThrustersActive)
        inputsUpdate.put("fore_thrusters", foreThrustersActive)
        inputsUpdate.put("aft_thrusters", aftThrustersActive)
        inputsUpdate.put("rotating_left", tillerLeft)
        inputsUpdate.put("rotating_right", tillerRight)
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
        val maxDockDist = 50.0//10000.0//50.0
        val maxDistSquared = maxDockDist.pow(2)

        val maxClosingSpeed = 500.0//5000.0//500.0
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