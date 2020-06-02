package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.ship.Ship
import java.lang.IllegalArgumentException
import java.util.*

class NavigationRoute(var ship: Ship, var shipPort: ShipDockingPort, var destination: StationDockingPort) {
    private val steps = LinkedList<NavigationState>()

    init {
        val startTime = 0.0
        val shipState = NavigationState(ship.globalPos, ship.rotation, ship.velocity)
        val bezierPhase = trainPhase(startTime) { endTimeEst ->
            val endVel = destination.velocityAtTime(endTimeEst)
            val endPortGlobalPos = destination.globalPosAtTime(endTimeEst)
            val myPortRelative = shipPort.relativePosition()
            val endRotation = destination.globalRotationAtTime(endTimeEst) + shipPort.relativeRotation()
            val targetPos = endPortGlobalPos + (myPortRelative * -1.0).rotated(endRotation)
            BezierPhase(startTime, ship, shipState, targetPos, endVel)
        }
        bezierPhase.computeStates(DHServer.tickLengthSeconds).forEach { steps.add(it) }
        val bezierDuration = steps.size * DHServer.tickLengthSeconds
        println("Bezier phase has duration $bezierDuration")

        val bezierEndState = steps.last
        val rotationStartTime = startTime + bezierDuration
        val rotationPhase = trainPhase(rotationStartTime){endTimeEst ->
            val stn = destination.station
            StationDockingRotationPhase(ship, rotationStartTime, bezierEndState, stn, stn.globalRotationAtTime(endTimeEst))
        }
        rotationPhase.computeStates(DHServer.tickLengthSeconds).forEach { steps.add(it) }
        val rotationDuration = steps.size * DHServer.tickLengthSeconds - bezierDuration
        println("Rotation has duration $rotationDuration")
    }

    fun getSteps(): LinkedList<NavigationState>
    {
        return steps
    }

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
            val newEstimate = currentPhase.endTime(DHServer.tickLengthSeconds)
            if (newEstimate.isNaN()) {
                throw IllegalStateException("Phase produced NaN time estimate.")
            }
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