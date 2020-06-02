package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.CommodityType
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.navigation.NavigationRoute
import com.dibujaron.distanthorizon.navigation.NavigationState
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.PlayerManager
import org.json.JSONObject
import java.lang.IllegalStateException
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

    private var manualControl = true
    private var route: SortedMap<Int, NavigationState> = TreeMap()

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

    var routeSetTime = 0L
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
            if (manualControl) {
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
                val stateIndex = route.firstKey()
                val state = route[stateIndex]!!
                route.remove(stateIndex)
                velocity = state.velocity
                globalPos = state.position
                rotation = state.rotation
                if (route.isEmpty()) {
                    val routeTime = System.currentTimeMillis() - routeSetTime
                    println("route complete in $routeTime ms")
                    completeDock()
                    manualControl = true
                }
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
        retval.put("docking_ports", myDockingPorts.asSequence().map { it.toJSON() }.toList())
        return retval
    }

    fun createShipHeartbeatJSON(): JSONObject {
        val retval = JSONObject()
        retval.put("id", uuid)
        retval.put("velocity", velocity.toJSON())
        retval.put("global_pos", globalPos.toJSON())
        retval.put("rotation", rotation)
        if (!manualControl) {
            retval.put("movement_script", createMovementScriptJSON())
        }
        //todo if not manually controlled send navigation steps
        return retval
    }

    fun createMovementScriptJSON(): JSONObject {
        val retval = JSONObject()
        if(manualControl){
            throw IllegalStateException("Manual control while creating movement script json!?")
        }
        route.asSequence().forEach { retval.put(it.key.toString(), it.value.toJSON()) }
        return retval
    }

    fun receiveInputChange(shipInputs: ShipInputs) {
        if (controls == shipInputs) {
            return;
        } else {
            controls = shipInputs
            broadcastInputsChange()
        }
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

    fun dockOrUndock() {
        if (dockedToPort == null) {
            navigateToDock()
        } else {
            if (dockedToPort != null) {
                DHServer.broadcastShipUndocked(this)
            }
            dockedToPort = null;
            myDockedPort = null;
        }
    }

    var navigatingToDockAtPort: StationDockingPort? = null
    var navigatingToDockFromPort: ShipDockingPort? = null
    fun navigateToDock() {
        println("attempting to navigate to dock.");
        val maxDockDist = 10000.0//10000.0//50.0
        val maxDistSquared = maxDockDist.pow(2)

        val maxClosingSpeed = 100000.0//5000.0//500.0
        val maxClosingSpeedSquared = maxClosingSpeed.pow(2)

        val match = OrbiterManager.getStations().asSequence()
            .flatMap { it.dockingPorts.asSequence() }
            .flatMap { stationPort ->
                myDockingPorts.asSequence()
                    .map { shipPort -> Pair(shipPort, stationPort) }
            }
            .filter{  it.first.relativeRotation() + it.second.relativeRotation == 0.0 } //only want docking position facing forward
            .filter { (it.first.getVelocity() - it.second.getVelocity()).lengthSquared < maxClosingSpeedSquared }
            .map { Triple(it.first, it.second, (it.first.globalPosition() - it.second.globalPosition()).lengthSquared) }
            .filter { it.third < maxDistSquared }
            .minBy { it.third }

        if (match != null) {
            println("Found docking match.")
            val bestShipPort = match.first
            val bestStationPort = match.second

            val navRoute = NavigationRoute(this, bestShipPort, bestStationPort)
            navRoute.getSteps().asSequence()
                .withIndex()
                .forEach { route[it.index] = it.value }

            navigatingToDockFromPort = bestShipPort
            navigatingToDockAtPort = bestStationPort
            routeSetTime = System.currentTimeMillis()
            println("route set, length is ${route.size}, first index is ${route.firstKey()}, manual control disabled.")
            manualControl = false
            broadcastInputsChange()
            //this.myDockedPort = bestShipPort
            //this.dockedToPort = bestStationPort
            //DHServer.broadcastShipDocked(this, bestShipPort, bestStationPort.station, bestStationPort);
        } else {
            println("Found no match to dock.")
        }
    }

    fun completeDock() {
        val shipPort = navigatingToDockFromPort
        val stationPort = navigatingToDockAtPort
        if (shipPort == null || stationPort == null) {
            throw IllegalStateException("Attempting to complete docking but I am missing docking targets. My port: $shipPort, station port: $stationPort")
        }
        this.myDockedPort = shipPort
        this.dockedToPort = stationPort
        navigatingToDockAtPort = null
        navigatingToDockFromPort = null
        println("docked to ${stationPort.station.name}");
        DHServer.broadcastShipDocked(this, shipPort, stationPort.station, stationPort);
    }
}
