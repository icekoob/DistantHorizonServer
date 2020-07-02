package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState
import java.util.*
const val RETRAIN_THRESHOLD = 60 * 5
class NavigationRoute(var ship: Ship, var shipPort: ShipDockingPort, var destination: StationDockingPort) {
    var currentPhase: BezierPhase = retrain()
    var ticksSinceRetrain = 0
    private fun retrain(): BezierPhase {
        return trainPhase(0) { endTickEst ->
            val endVel = destination.velocityAtTick(endTickEst)
            val endPortGlobalPos = destination.globalPosAtTick(endTickEst)
            val myPortRelative = shipPort.relativePosition()
            val endRotation = destination.globalRotationAtTick(endTickEst) + shipPort.relativeRotation() //why do I keep having to offset by pi here
            val targetPos = endPortGlobalPos + (myPortRelative * -1.0).rotated(endRotation)
            val endState = ShipState(targetPos, endRotation, endVel)
            //lastEstEndTime = endTimeEst
            BezierPhase(0.0, ship.type.mainThrust, ship.currentState, endState)
        }
    }

    fun getEndState(): ShipState{
        return currentPhase.getEndState()
    }

    fun hasNext(delta: Double): Boolean
    {
        return currentPhase.hasNextStep(delta)
    }

    fun next(delta: Double): ShipState
    {
        /*if(ticksSinceRetrain > RETRAIN_THRESHOLD){
            currentPhase = retrain()
            ticksSinceRetrain = 0
        } else {
            ticksSinceRetrain++
        }*/
        return currentPhase.step(delta)
    }

    fun getDiagnostic(): String
    {
        return "asdf"
    }

    companion object {

        protected fun trainPhase(
            startTick: Int,
            endTickToPhaseFunc: (endTick: Double) -> BezierPhase
        ): BezierPhase {
            var iterations = 0
            val previousEstimations = LinkedList<Double>()
            previousEstimations.addLast(startTick.toDouble())
            while (iterations < 100000) {
                if (iterations > 6) {
                    previousEstimations.pollFirst()
                }
                val previousEstimate = previousEstimations.last
                val currentPhase = endTickToPhaseFunc(previousEstimate)
                val newEstimate = startTick + currentPhase.durationTicks
                /*if (newEstimate.isNaN()) {
                    throw IllegalStateException("Phase produced NaN time estimate.")
                }*/
                val movingAverage = previousEstimations.asSequence().sum() / previousEstimations.size
                val diff = newEstimate - movingAverage
                if (diff < 0.1) {
                    return currentPhase
                }
                previousEstimations.addLast(newEstimate)
                iterations++
            }
            throw IllegalStateException("Phase training failed to converge.")
        }
    }
}