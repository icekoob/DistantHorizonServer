package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.orbiter.CommodityType
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.Account
import com.dibujaron.distanthorizon.ship.controller.ShipController
import org.json.JSONObject
import java.awt.Color
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

class Ship(
    val type: ShipClass,
    val primaryColor: ShipColor,
    val secondaryColor: ShipColor,
    initialState: ShipState,
    private val controller: ShipController
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

    var tickCount = 0

    fun process(delta: Double, coursePlottingAllowed: Boolean) {
        val dockedTo = dockedToPort
        val dockedFrom = myDockedPort
        val startTime = System.currentTimeMillis()
        if (dockedTo != null && dockedFrom != null) {
            val velocity = dockedTo.getVelocity()
            val myPortRelative = dockedFrom.relativePosition()
            val rotation = dockedTo.globalRotation() + dockedFrom.relativeRotation()
            val globalPos = dockedTo.globalPosition() + (myPortRelative * -1.0).rotated(rotation)
            currentState = ShipState(globalPos, rotation, velocity)
            controller.dockedTick(delta, coursePlottingAllowed)
        } else {
            currentState = controller.computeNextState(delta)
        }
        tickCount++
    }

    fun createFullShipJSON(): JSONObject {
        val retval = createShipHeartbeatJSON()
        val controls = controller.getCurrentControls()
        retval.put("type", type.qualifiedName)
        retval.put("hold_size", type.holdSize)
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
        retval.put("primary_color", primaryColor.toJSON())
        retval.put("secondary_color", secondaryColor.toJSON())
        retval.put("docking_ports", myDockingPorts.asSequence().map { it.toJSON() }.toList())
        return retval
    }

    fun createShipHeartbeatJSON(): JSONObject {
        val retval = JSONObject()
        retval.put("id", uuid)
        retval.put("velocity", currentState.velocity.toJSON())
        retval.put("global_pos", currentState.position.toJSON())
        retval.put("rotation", currentState.rotation)
        retval.put("hold_occupied", controller.getHoldOccupied())
        val navigating = controller.navigatingToTarget()
        retval.put("navigating", navigating)
        if(navigating){
            val target = controller.getNavTarget()
            retval.put("targ_velocity", target.velocity.toJSON())
            retval.put("targ_position", target.position.toJSON())
            retval.put("targ_rotation", target.rotation)
        }
        //todo if not manually controlled send navigation steps
        return retval
    }

    fun attemptDock() {
        val maxDockDist = 500.0//10000.0//10000.0//50.0
        val maxDistSquared = maxDockDist.pow(2)

        val maxClosingSpeed = 500.0//100000.0//5000.0//500.0
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
            val bestShipPort = match.first
            val bestStationPort = match.second
            dock(bestShipPort, bestStationPort)
        } else {
            println("Found no match to dock.")
        }
    }

    fun dock(shipPort: ShipDockingPort, stationPort: StationDockingPort) {
        this.myDockedPort = shipPort
        this.dockedToPort = stationPort
        DHServer.broadcastShipDocked(this, shipPort, stationPort.station, stationPort);
    }

    fun undock() {
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
