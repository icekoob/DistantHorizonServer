package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.navigation.NavigationRoute
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.IndexedState
import com.dibujaron.distanthorizon.ship.ShipInputs
import com.dibujaron.distanthorizon.ship.ShipManager
import com.dibujaron.distanthorizon.ship.ShipState
import java.lang.IllegalStateException
import kotlin.math.roundToInt

class AIShipController : ShipController() {

    var nextDepartureTime = System.currentTimeMillis()
    var currentRoute: NavigationRoute? = null

    override fun dockedTick(delta: Double) {
        if (System.currentTimeMillis() > nextDepartureTime) {
            plotNewCourse()
            ship.undock()
        }
    }

    fun plotNewCourse() {
        val stations = OrbiterManager.getStations().asSequence().filter {
            !ship.isDocked() || ship.dockedToPort!!.station != it
        }.filter{ it.getStar().name == "S-Regalis" }.toList()
        val destStation = stations.random()
        val destPort = destStation.dockingPorts.random()
        val myPort = ship.myDockingPorts.random()
        val newRoute = NavigationRoute(ship, myPort, destPort)
        println("${ship.uuid} departing for ${destPort.station.displayName}")
        currentRoute = newRoute
    }

    fun dock() {
        ship.attemptDock()
        if (ship.isDocked()) {
            val currentTime = System.currentTimeMillis()
            nextDepartureTime = currentTime + 5000 + (Math.random() * 1000).roundToInt()
            val waitTime = nextDepartureTime - System.currentTimeMillis()
            println("AI ship ${ship.uuid} successfully docked at ${ship.dockedToPort!!.station.displayName}, will depart again after ${waitTime}ms")
            println("current time is ${DHServer.timeSinceStart()}")
        } else {
            println("AI ship ${ship.uuid} should have docked but failed to dock, removing ship.")
            ShipManager.markForRemove(ship)
        }
    }

    override fun computeNextState(delta: Double): ShipState {
        val route = currentRoute
        if (route != null && route.hasNext(delta)) {
            return route.next(delta)
        } else {
            if(route != null) {
                val dist = (route.destination.globalPosition() - ship.currentState.position).length
                val distFromEndState = (route.getEndState().position - ship.currentState.position).length
                println("route complete. true destination position is ${route.destination.globalPosition()}. Current time is ${DHServer.timeSinceStart()}")
                println("dist = $dist. end state dist = $distFromEndState")
            }
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