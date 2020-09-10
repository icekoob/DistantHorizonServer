package com.dibujaron.distanthorizon.ship.controller.ai.nav

import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipState

//a simple form of navigation
//use if steps do not gain any efficiency from being calculated simultaneously.
//also does no caching.
abstract class IndependentStepsNavigation(
    startState: ShipState,
    targetState: ShipState,
    targetStation: Station,
    targetStationPort: StationDockingPort,
    myPort: ShipDockingPort
) : Navigation(startState, targetState, targetStation, targetStationPort, myPort) {

    abstract fun getStep(step: Int): ShipState

    override fun getSteps(startStep: Int, numSteps: Int): Sequence<ShipState> {
        val endStep = startStep + numSteps
        return (startStep..endStep).asSequence().map { getStep(it) }
    }
}