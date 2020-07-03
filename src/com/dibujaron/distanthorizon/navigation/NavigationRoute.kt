package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState
import java.util.*

class NavigationRoute(var ship: Ship, var shipPort: ShipDockingPort, var destination: StationDockingPort) {
    var currentPhase: BezierNavigationPhase = trainPhase(0) { endTickEst ->
            val endVel = destination.velocityAtTick(endTickEst)
            val endPortGlobalPos = destination.globalPosAtTick(endTickEst)
            val myPortRelative = shipPort.relativePosition()
            val endRotation = destination.globalRotationAtTick(endTickEst) + shipPort.relativeRotation() //why do I keep having to offset by pi here
            val targetPos = endPortGlobalPos + (myPortRelative * -1.0).rotated(endRotation)
            val endState = ShipState(targetPos, endRotation, endVel)
            BezierNavigationPhase(ship.type.mainThrust, ship.currentState, endState)
        }

    fun getEndState(): ShipState{
        return currentPhase.endState
    }

    fun hasNext(delta: Double): Boolean
    {
        return currentPhase.hasNextStep()
    }

    fun next(delta: Double): ShipState
    {
        return currentPhase.step()
    }

    fun getDiagnostic(): String
    {
        return "asdf"
    }

    companion object {

        fun trainPhase(
            startTick: Int,
            endTickToPhaseFunc: (endTick: Double) -> BezierNavigationPhase
        ): BezierNavigationPhase {
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
                if (diff < 0.01) {
                    return currentPhase
                }
                previousEstimations.addLast(newEstimate)
                iterations++
            }
            throw IllegalStateException("Phase training failed to converge.")
        }
    }
}