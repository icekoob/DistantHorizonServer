package com.dibujaron.distanthorizon.pathfinder

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

class PathState(
    val execution: PathExecution,
    val priorState: PathState?,
    val timeSteps: Int,
    val globalPos: Vector2,
    val velocity: Vector2,
    val globalRot: Double,
    val inputs: PathInputs
) {
    val time: Double by lazy {timeSteps * execution.timeStepLength}
    val f: Double by lazy {g + h}
    val g: Double = time
    val h: Double by lazy{ calculateBestTravelTimeToDestFromMe()}
    //constructor(s: PathState): this(s.priorState, s.timeSteps, s.globalPos, s.velocity, s.globalRot, s.inputs)

    fun calculateBestTravelTimeToDestFromMe(): Double
    {
        var travelTime = 0.0
        var iterations = 0
        val pastResultsList = LinkedList<Double>()
        while(iterations < ITERATION_LIMIT){
            val arrivalTime = time + travelTime
            val end = execution.end.globalPosAtTime(arrivalTime)
            val dist = (end - globalPos).length
            val accelDist = dist / 2
            val accelTime = sqrt(accelDist / (0.5 * execution.maxAcceleration))
            travelTime = accelTime * 2
            pastResultsList.addFirst(travelTime)
            if(iterations > PAST_RESULTS_SIZE){
                val avg = pastResultsList.asSequence().sum() / pastResultsList.size
                val diff = abs(avg - travelTime)
                if(diff < 0.1){
                    break
                }
                pastResultsList.pop()
            }
            iterations++
        }
        return travelTime
    }

    fun roughlyEqualTo(other: PathState): Boolean
    {
        if(timeSteps != other.timeSteps){
            return false;
        }
        val positionDiffSquared = (globalPos - other.globalPos).lengthSquared
        if(positionDiffSquared > 100){
            return false;
        }
        val velocityDiffSquared = (velocity - other.velocity).lengthSquared
        return velocityDiffSquared < 100
    }

    fun nextStates(delta: Double): Sequence<PathState>
    {
        val nextPos = globalPos + velocity
        var nextVelocity = velocity
        if(inputs.enginesActive){
            nextVelocity += Vector2(0, -execution.mainEngineThrust).rotated(globalRot) * delta
        }
        nextVelocity += OrbiterManager.calculateGravity(time, globalPos)
        var nextRotation = globalRot
        if(inputs.rotatingLeft){
            nextRotation -= execution.rotationPower * delta
        }
        if(inputs.rotatingRight){
            nextRotation += execution.rotationPower * delta
        }

        return PathInputs.allInputCombinations.asSequence().map{
            PathState(execution, this, timeSteps+1, nextPos, nextVelocity, nextRotation, it)
        }
    }

    companion object {
        const val PAST_RESULTS_SIZE = 5
        const val ITERATION_LIMIT = 100000
    }

}