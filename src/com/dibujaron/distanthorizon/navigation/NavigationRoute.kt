package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState
import java.util.*
//const val RETRAIN_THRESHOLD = 60
class NavigationRoute(var ship: Ship, var shipPort: ShipDockingPort, var destination: StationDockingPort) {
    var currentPhase: NavigationPhase = retrain()
    private fun retrain(): NavigationPhase{
        val retval = trainPhase(0.0) { endTimeEst ->
            val endVel = destination.velocityAtTime(endTimeEst)
            val endPortGlobalPos = destination.globalPosAtTime(endTimeEst)
            val myPortRelative = shipPort.relativePosition()
            val endRotation = destination.globalRotationAtTime(endTimeEst) + shipPort.relativeRotation()
            val targetPos = endPortGlobalPos + (myPortRelative * -1.0).rotated(endRotation)
            val endState = ShipState(targetPos, endRotation, endVel)
            //lastEstEndTime = endTimeEst
            BezierPhase(0.0, ship, ship.currentState, endState)
        }
        //val estArrivalTime = DHServer.timeSinceStart() + (lastEstEndTime * 1000)
        //println("retrained. target pos is ${retval.getEndState().position}, will arrive at $estArrivalTime")
        return retval
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
        return currentPhase.step(delta)
    }

    fun getDiagnostic(): String
    {
        val cp = currentPhase
        if(cp is BezierPhase){
            return cp.diagnosticBuilder.toString()
        } else {
            return "current phase is ${cp.javaClass}";
        }
    }

    companion object {

        protected fun trainPhase(
            startTime: Double,
            endTimeToPhaseFunc: (endTime: Double) -> NavigationPhase
        ): NavigationPhase {
            var iterations = 0
            val previousEstimations = LinkedList<Double>()
            previousEstimations.addLast(startTime)
            while (iterations < 100000) {
                if (iterations > 6) {
                    previousEstimations.pollFirst()
                }
                val previousEstimate = previousEstimations.last
                val currentPhase = endTimeToPhaseFunc(previousEstimate)
                val newEstimate = currentPhase.endTime(DHServer.TICK_LENGTH_SECONDS)
                if (newEstimate.isNaN()) {
                    throw IllegalStateException("Phase produced NaN time estimate.")
                }
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