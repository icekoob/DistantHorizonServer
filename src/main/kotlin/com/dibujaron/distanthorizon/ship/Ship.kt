package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.docking.DockingPort
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.CommodityType
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.player.Account
import com.dibujaron.distanthorizon.ship.controller.ShipController
import org.json.JSONObject
import java.util.*
import kotlin.math.pow

class Ship(
    val type: ShipClass,
    private val primaryColor: ShipColor,
    private val secondaryColor: ShipColor,
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

    fun tick() {
        val dockedTo = dockedToPort
        val dockedFrom = myDockedPort
        if (dockedTo != null && dockedFrom != null) {
            if(controller.isRequestingUndock()){
                undock()
                currentState = controller.computeNextState()
            } else {
                val velocity = dockedTo.getVelocity()
                val myPortRelative = dockedFrom.relativePosition()
                val rotation = dockedTo.globalRotation() + dockedFrom.relativeRotation()
                val globalPos = dockedTo.globalPosition() + (myPortRelative * -1.0).rotated(rotation)
                currentState = ShipState(globalPos, rotation, velocity)
            }
        } else {
            currentState = controller.computeNextState()
        }
        tickCount++
    }

    fun createFullShipJSON(): JSONObject {
        val retval = createShipHeartbeatJSON()
        retval.put("type", type.qualifiedName)
        retval.put("hold_size", type.holdSize)
        retval.put("main_engine_thrust", type.mainThrust)
        retval.put("manu_engine_thrust", type.manuThrust)
        retval.put("rotation_power", type.rotationPower)
        retval.put("primary_color", primaryColor.toJSON())
        retval.put("secondary_color", secondaryColor.toJSON())
        retval.put("docking_ports", myDockingPorts.asSequence().map { it.toJSON() }.toList())
        retval.put("docked", isDocked())
        if(isDocked()){
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
        retval.put("controller_type", controller.getType().toJSON())
        retval.put("controller_heartbeat", controller.getHeartbeat())
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
            .minBy { it.third }

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
    }

    fun createDockedMessage(): JSONObject
    {
        val myPort: DockingPort? = this.myDockedPort
        val stationPort: StationDockingPort? = this.dockedToPort;
        if(myPort == null || stationPort == null){
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
