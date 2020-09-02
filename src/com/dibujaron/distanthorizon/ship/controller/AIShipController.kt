package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.navigation.NavigationRoute
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.*
import org.json.JSONObject
import kotlin.math.roundToInt

class AIShipController : ShipController() {

    val ONLY_MAIN_SYSTEM = true
    var nextDepartureTime = computeNextDeparture()
    var currentRoute: NavigationRoute? = null

    override fun undockRequested(delta: Double): Boolean {
        return if (System.currentTimeMillis() > nextDepartureTime) {
            plotNewCourse()
            true;
        } else {
            false;
        }
    }

    fun plotNewCourse() {
        val stations = OrbiterManager.getStations().asSequence().filter {
            !ship.isDocked() || ship.dockedToPort!!.station != it
        }.filter { !ONLY_MAIN_SYSTEM || it.getStar().name == "S-Regalis" }.toList()
        val destStation = stations.random()
        val destPort = destStation.dockingPorts.random()
        val myPort = ship.myDockingPorts.random()
        val newRoute = NavigationRoute(ship, myPort, destPort)
        currentRoute = newRoute
    }

    fun dock() {
        ship.attemptDock(2000.0, 2000.0)
        if (ship.isDocked()) {
            nextDepartureTime = computeNextDeparture()
        } else {
            println("AI ship ${ship.uuid} should have docked but failed to dock.")
            ShipManager.markForRemove(ship)
            //spawn a new one
            ShipManager.markForAdd(
                Ship(
                    ShipClassManager.getShipClasses().random(),
                    ShipColor.random(),
                    ShipColor.random(),
                    currentRoute!!.destination.station.getState(),
                    AIShipController()
                )
            )
        }
    }

    fun dock(myPort: ShipDockingPort, targetPort: StationDockingPort) {
        ship.dock(myPort, targetPort)
        if (ship.isDocked()) {
            nextDepartureTime = computeNextDeparture()
        } else {
            println("AI ship ${ship.uuid} should have docked but failed to dock.")
            ShipManager.markForRemove(ship)
            //spawn a new one
            ShipManager.markForAdd(
                Ship(
                    ShipClassManager.getShipClasses().random(),
                    ShipColor.random(),
                    ShipColor.random(),
                    currentRoute!!.destination.station.getState(),
                    AIShipController()
                )
            )
        }
    }

    override fun computeNextState(delta: Double): ShipState {
        val route = currentRoute
        return if (route != null && route.hasNext(delta)) {
            route.next(delta)
        } else {
            if (route != null) {
                dock(route.shipPort, route.destination)
            } else {
                dock()
            }
            ship.currentState
        }
    }

    override fun getType(): ControllerType {
        return ControllerType.ARTIFICIAL
    }

    override fun getHeartbeat(): JSONObject {
        return JSONObject()
    }

    companion object {
        fun computeNextDeparture(): Long {
            return System.currentTimeMillis() + 5000 + (Math.random() * 10000).roundToInt()
        }
    }
}