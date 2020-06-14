package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.navigation.NavigationRoute
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.*
import java.lang.IllegalStateException
import kotlin.math.roundToInt

class AIShipController : ShipController() {

    var nextDepartureTime = System.currentTimeMillis()
    var currentRoute: NavigationRoute? = null
    var fakeHoldOccupied: Int = 0
    override fun dockedTick(delta: Double) {
        if (System.currentTimeMillis() > nextDepartureTime) {
            plotNewCourse()
            fakeHoldOccupied = (Math.random() * ship.holdCapacity).toInt()
            ship.undock()
        }
    }

    fun plotNewCourse() {
        val stations = OrbiterManager.getStations().asSequence().filter {
            !ship.isDocked() || ship.dockedToPort!!.station != it
        }/*.filter{ it.getStar().name == "S-Regalis" }*/.toList()
        val destStation = stations.random()
        val destPort = destStation.dockingPorts.random()
        val myPort = ship.myDockingPorts.random()
        val newRoute = NavigationRoute(ship, myPort, destPort)
        currentRoute = newRoute
    }

    fun dock() {
        ship.attemptDock()
        if (ship.isDocked()) {
            val currentTime = System.currentTimeMillis()
            nextDepartureTime = currentTime + 5000 + (Math.random() * 1000).roundToInt()
        } else {
            println("AI ship ${ship.uuid} should have docked but failed to dock, removing ship.")
            ShipManager.markForRemove(ship)
            //spawn a new one
            ShipManager.markForAdd(Ship(ShipClassManager.getShipClasses().random(), ShipColor.random(), ShipColor.random(), currentRoute!!.destination.station.getState(), AIShipController()))
        }
    }

    override fun getHoldOccupied(): Int {
        return fakeHoldOccupied
    }

    override fun computeNextState(delta: Double): ShipState {
        val route = currentRoute
        if (route != null && route.hasNext(delta)) {
            return route.next(delta)
        } else {
            dock()
            return ship.currentState
        }
    }

    override fun getCurrentControls(): ShipInputs {
        return ShipInputs()
    }

    override fun navigatingToTarget(): Boolean {
        return !ship.isDocked()
    }

    //this is called when we send our heartbeat. It'd be a good time to recalculate.
    override fun getNavTarget(): ShipState {
        val route = currentRoute
        if(route == null){
            throw IllegalStateException("not navigating currently")
        } else {
            return route.getEndState()
        }
    }
}