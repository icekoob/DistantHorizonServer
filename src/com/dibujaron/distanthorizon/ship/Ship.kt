package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.orbiter.CommodityType
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.Account
import com.dibujaron.distanthorizon.ship.controller.ShipController
import org.json.JSONObject
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

class Ship(
    val type: ShipClass,
    initialState: ShipState,
    val controller: ShipController
) {

    init {
        controller.initForShip(this)
    }

    var currentState: ShipState = initialState
    val uuid = UUID.randomUUID()

    //controls
    val myDockingPorts = type.dockingPorts.asSequence().map { ShipDockingPort(this, it) }.toList()
    var dockedToPort: StationDockingPort? = null
    var myDockedPort: ShipDockingPort? = null

    var holdCapacity = type.holdSize
    var hold = HashMap<String, Int>()

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

    var routeSetTime = 0L
    var tickCount = 0

    fun process(delta: Double) {
        val dockedTo = dockedToPort
        val dockedFrom = myDockedPort
        if (dockedTo != null && dockedFrom != null) {
            val velocity = dockedTo.getVelocity()
            val myPortRelative = dockedFrom.relativePosition()
            val rotation = dockedTo.globalRotation() + dockedFrom.relativeRotation()
            val globalPos = dockedTo.globalPosition() + (myPortRelative * -1.0).rotated(rotation)
            currentState = ShipState(globalPos, rotation, velocity)
        } else {
            currentState = controller.next(delta, currentState)
        }
        tickCount++
    }

    fun createFullShipJSON(): JSONObject {
        val retval = createShipHeartbeatJSON()
        val controls = controller.getCurrentControls()
        retval.put("type", type.qualifiedName)
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
        retval.put("velocity", currentState.velocity.toJSON())
        retval.put("global_pos", currentState.position.toJSON())
        retval.put("rotation", currentState.rotation)
        retval.put("movement_script", createMovementScriptJSON())
        //todo if not manually controlled send navigation steps
        return retval
    }

    fun createMovementScriptJSON(): JSONObject {
        val retval = JSONObject()
        val tps = (1 / DHServer.tickLengthSeconds).roundToInt()
        controller.publishScript(tps * 5).forEach { retval.put(it.index.toString(), it.state.toJSON()) }
        return retval
    }

    fun attemptDock() {
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
            .filter { it.first.relativeRotation() + it.second.relativeRotation == 0.0 } //only want docking position facing forward
            .filter { (it.first.getVelocity() - it.second.getVelocity()).lengthSquared < maxClosingSpeedSquared }
            .map { Triple(it.first, it.second, (it.first.globalPosition() - it.second.globalPosition()).lengthSquared) }
            .filter { it.third < maxDistSquared }
            .minBy { it.third }

        if (match != null) {
            println("Found docking match.")
            val bestShipPort = match.first
            val bestStationPort = match.second
            completeDock(bestShipPort, bestStationPort)
        } else {
            println("Found no match to dock.")
        }
    }

    fun completeDock(shipPort: ShipDockingPort, stationPort: StationDockingPort) {
        this.myDockedPort = shipPort
        this.dockedToPort = stationPort
        println("docked to ${stationPort.station.name}");
        DHServer.broadcastShipDocked(this, shipPort, stationPort.station, stationPort);
    }

    fun completeUndock() {
        if (dockedToPort != null) {
            DHServer.broadcastShipUndocked(this)
        }
        dockedToPort = null;
        myDockedPort = null;
    }

    fun buyResourceFromStation(commodity: String, purchasingAccount: Account, quantity: Int){
        if(isDocked()) {
            val station = dockedToPort!!.station
            station.sellResourceToShip(commodity,purchasingAccount, this, quantity)
        }
    }

    fun sellResourceToStation(commodity: String, purchasingAccount: Account, quantity: Int)
    {
        if(isDocked()) {
            val station = dockedToPort!!.station
            station.buyResourceFromShip(commodity,purchasingAccount, this, quantity)
        }
    }
}
