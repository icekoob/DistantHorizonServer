package com.dibujaron.distanthorizon.ship.controller.ai

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.controller.ai.nav.IndependentStepsNavigation
import com.dibujaron.distanthorizon.ship.controller.ai.nav.Navigation

const val UNITS_PER_TICK = 500.0

//dead simple controller that drives the ship in a straight line at a constant speed from A to B.
//makes no attempt to match velocities.
class SimpleStraightLineController : NovelRoutingAIController() {

    override fun getMaximumStationDwellTicks(): Int {
        return 60 * 10
    }

    override fun getMinimumStationDwellTicks(): Int {
        return 60 * 5
    }

    override fun plotNewCourse(): Navigation {
        val n = super.plotNewCourse()
        println("ship ${ship.uuid} is departing from ${ship.dockedToPort?.station}")
        return n
    }

    override fun createNavigation(startState: ShipState, targetState: ShipState, targetStation: Station, targetStationPort: StationDockingPort, myPort: ShipDockingPort): Navigation {
        return StraightLineNavigation(UNITS_PER_TICK, startState, targetState, targetStation, targetStationPort, myPort)
    }

    class StraightLineNavigation(
        private val speed: Double,
        startState: ShipState,
        targetState: ShipState,
        targetStation: Station,
        targetStationPort: StationDockingPort,
        myPort: ShipDockingPort
    ) : IndependentStepsNavigation(startState, targetState, targetStation, targetStationPort, myPort) {

        private val diff = (targetState.position - startState.position)
        private val dist = diff.length
        private val angle = diff.angle
        private val dx = diff.x / speed
        private val dy = diff.y / speed
        private val velocity = Vector2(dx, dy)

        override fun getStep(step: Int): ShipState {
            val pos = startState.position + (velocity * step)
            return ShipState(pos, angle, velocity)
        }

        override fun numStepsExact(): Double {
            //d = rt
            //t = d / r
            return dist / speed
        }

    }
}