package com.dibujaron.distanthorizon.ship.controller

import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.navigation.NavigationRoute
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.*
import java.lang.IllegalStateException
import java.lang.StringBuilder
import kotlin.math.roundToInt

class AIShipController : ShipController() {

    val ONLY_MAIN_SYSTEM = true
    var nextDepartureTime = computeNextDeparture()
    var currentRoute: NavigationRoute? = null
    var fakeHoldOccupied: Int = 0

    var diagnosticBuilder = StringBuilder()
    override fun getDiagnostic(): String {
        val cr = currentRoute;
        if(cr != null){
            return "active: " + cr.getDiagnostic()
        } else {
            return "docked: " + diagnosticBuilder.toString();
        }
    }

    override fun shouldUndock(delta: Double, coursePlottingAllowed: Boolean): Boolean {
        if (System.currentTimeMillis() > nextDepartureTime && coursePlottingAllowed) {
            plotNewCourse()
            fakeHoldOccupied = (Math.random() * ship.holdCapacity).toInt()
            return true;
        } else {
            return false;
        }
    }

    fun plotNewCourse() {
        val stations = OrbiterManager.getStations().asSequence().filter {
            !ship.isDocked() || ship.dockedToPort!!.station != it
        }.filter{ !ONLY_MAIN_SYSTEM || it.getStar().name == "S-Regalis" }.toList()
        val destStation = stations.random()
        val destPort = destStation.dockingPorts.random()
        val myPort = ship.myDockingPorts.random()
        val newRoute = NavigationRoute(ship, myPort, destPort)
        currentRoute = newRoute
        ticksNavigating = 0
        targetTicks = destStation.ticks
    }

    fun dock(){
        ship.attemptDock(2000.0, 2000.0)
        if (ship.isDocked()) {
            nextDepartureTime = computeNextDeparture()
        } else {
            println("AI ship ${ship.uuid} should have docked but failed to dock.")
            ShipManager.markForRemove(ship)
            //spawn a new one
            ShipManager.markForAdd(Ship(ShipClassManager.getShipClasses().random(), ShipColor.random(), ShipColor.random(), currentRoute!!.destination.station.getState(), AIShipController()))
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
            ShipManager.markForAdd(Ship(ShipClassManager.getShipClasses().random(), ShipColor.random(), ShipColor.random(), currentRoute!!.destination.station.getState(), AIShipController()))
        }
    }

    override fun getHoldOccupied(): Int {
        return fakeHoldOccupied
    }

    var ticksNavigating = 0
    var targetTicks = 0
    override fun computeNextState(delta: Double): ShipState {
        val route = currentRoute
        if (route != null && route.hasNext(delta)) {
            ticksNavigating++
            return route.next(delta)
        } else {
            if(route != null /*&& route.destination.station.name == "Stn_Innerstellar Launch"*/){
                val distToTarget = (route.getEndState().position - ship.currentState.position).length.roundToInt()
                val expectedDockingPosition = route.destination.globalPosition() + (route.shipPort.relativePosition() * -1.0).rotated(route.getEndState().rotation)
                val trueError = (expectedDockingPosition - ship.currentState.position).length.roundToInt()
                val elapsedTime = (route.currentPhase.ticksSinceStart)
                val targetElapsedTicks = route.destination.station.ticks - targetTicks
                val tickDiff = ticksNavigating - targetElapsedTicks
                val destinationTravelInTick = (route.destination.station.globalPosAtTick(1.0) - route.destination.globalPosAtTick(0.0)).length.roundToInt()
                println("route complete. target error=$distToTarget, true error=$trueError, tickDiff=$tickDiff, target travels $destinationTravelInTick in one tick")
                dock(route.shipPort, route.destination)
            } else {
                dock()
            }
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
    companion object {
        fun computeNextDeparture(): Long
        {
            return System.currentTimeMillis() + 5000 + (Math.random() * 10000).roundToInt()
        }
    }
}