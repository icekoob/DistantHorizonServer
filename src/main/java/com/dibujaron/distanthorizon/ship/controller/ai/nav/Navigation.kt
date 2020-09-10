package com.dibujaron.distanthorizon.ship.controller.ai.nav

import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipState
import kotlin.math.ceil

abstract class Navigation(
    val startState: ShipState,
    val targetState: ShipState,
    val targetStation: Station,
    val targetStationPort: StationDockingPort,
    val myPort: ShipDockingPort
) {

    abstract fun getSteps(startStep: Int, numSteps: Int): Sequence<ShipState>
    abstract fun numStepsExact(): Double

    open fun numSteps(): Int {
        return ceil(numStepsExact()).toInt()
    }

}