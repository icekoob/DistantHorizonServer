package com.dibujaron.distanthorizon.ship.controller.ai

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.controller.ai.nav.IndependentStepsNavigation
import com.dibujaron.distanthorizon.ship.controller.ai.nav.Navigation

const val UNITS_PER_TICK = 1.0/60.0

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
        println("ship ${ship.uuid} is departing from ${ship.dockedToPort?.station} to ${n.targetStation}")
        println("    departed from ${n.startState.position}, heading to ${n.targetState.position}")
        return n
    }

    override fun createNavigation(startState: ShipState, targetState: ShipState, targetStation: Station, targetStationPort: StationDockingPort, myPort: ShipDockingPort): Navigation {
        var n = StraightLineNavigation(UNITS_PER_TICK, startState, targetState, targetStation, targetStationPort, myPort)
        //println("navigation should take ${n.numStepsExact()} steps, ${n.numStepsExact() / 60} seconds.")
        return n
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
        private val dx = diff.x * speed
        private val dy = diff.y * speed
        private val velocityPerTick = Vector2(dx, dy)

        override fun getStep(step: Int): ShipState {
            if(step == 0){
                println("    velocity per tick is $velocityPerTick")
            }
            val pos = startState.position + (velocityPerTick * step)
            return ShipState(pos, angle, velocityPerTick)
        }

        override fun numStepsExact(): Double {
            return dist / speed
        }

    }
}