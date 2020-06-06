package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.ship.IndexedState
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState
import org.json.JSONObject
import java.util.*

class NavigationRoute(var ship: Ship, var shipPort: ShipDockingPort, var destination: StationDockingPort) {
    private val steps = TreeMap<Int, ShipState>()
    var maxStepIndex = -1
    init {
        val startTime = 0.0
        val shipState = ship.currentState
        var t = System.currentTimeMillis()
        val bezierPhase = trainPhase(startTime) { endTimeEst ->
            val endVel = destination.velocityAtTime(endTimeEst)
            val endPortGlobalPos = destination.globalPosAtTime(endTimeEst)
            val myPortRelative = shipPort.relativePosition()
            val endRotation = destination.globalRotationAtTime(endTimeEst) + shipPort.relativeRotation()
            val targetPos = endPortGlobalPos + (myPortRelative * -1.0).rotated(endRotation)
            BezierPhase(startTime, ship, shipState, targetPos, endVel)
        }
        println("phase training took ${System.currentTimeMillis() - t}ms")
        t = System.currentTimeMillis()
        bezierPhase.computeStates(DHServer.tickLengthSeconds).forEach { steps[++maxStepIndex] = it}
        println("Computing states took ${System.currentTimeMillis() - t}ms")
        val bezierDuration = steps.size * DHServer.tickLengthSeconds
        println("Bezier phase has duration $bezierDuration")
    }

    fun publishScript(numToWrite: Int): Sequence<IndexedState>
    {
        val currentStep = steps.firstKey()
        var endStep = currentStep + numToWrite

        if(endStep > maxStepIndex){
            endStep = maxStepIndex
        }
        return (currentStep..endStep).asSequence().map {
            IndexedState(it, steps[it]!!)
        }
    }

    fun currentStep(): Int {
        return steps.firstKey()
    }

    fun hasNext(): Boolean
    {
        return !steps.isEmpty()
    }

    fun next(): ShipState
    {
        val stateIndex = steps.firstKey()
        val state = steps[stateIndex]!!
        steps.remove(stateIndex)
        return state
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
            if (diff < 0.05) {
                return currentPhase
            }
            previousEstimations.addLast(newEstimate)
            iterations++
        }
        throw IllegalStateException("Phase training failed to converge.")
    }
}