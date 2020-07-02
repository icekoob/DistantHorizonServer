package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.bezier.BezierCurve
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState
import java.lang.IllegalStateException
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

class BezierPhase(val startTime: Double, val mainThrust: Double, val startState: ShipState, private val targetState: ShipState){

    //a phase that navigates a smooth curve from startPos with startVel to endPos with endVel
    //makes use of a Bezier Curve.

    val curve: BezierCurve = BezierCurve.fromStates(startState, targetState, 100)
    var ticksSinceStart = 0
    val timeToFlip by lazy { timeToFlipPoint() }
    val duration by lazy { computeDuration() }
    val durationTicks by lazy {duration * DHServer.TICKS_PER_SECOND}

    fun getEndState(): ShipState {
        return targetState
    }

    //called by lazy
    fun computeDuration(): Double {
        //in curve space, target vel and start vel are always pointing the same direction
        val endSpeed = targetState.velocity.length
        val startSpeed = startState.velocity.length
        val curveLength = curve.length
        val maxAccel = mainThrust
        val accelTime = timeToFlip
        val distToFlipPoint = distanceFromStartToFlipPoint(startSpeed, endSpeed, maxAccel, curveLength)
        val decelDist = curveLength - distToFlipPoint
        //the time it would take to accelerate from end to flip point
        //should be the same as the time it takes to decelerate from flip point to end.
        val decelTime = travelTimeConstantAccel(endSpeed, maxAccel, decelDist)
        return accelTime + decelTime
    }

    //called by lazy
    private fun timeToFlipPoint(): Double {
        val endSpeed = targetState.velocity.length
        val startSpeed = startState.velocity.length
        val curveLength = curve.length
        val maxAccel = mainThrust
        val distToFlipPoint = distanceFromStartToFlipPoint(startSpeed, endSpeed, maxAccel, curveLength)
        return travelTimeConstantAccel(startSpeed, maxAccel, distToFlipPoint)
    }


    fun hasNextStep(delta: Double): Boolean {
        //return totalDistSoFar(timeOffsetFromStart + delta) <= curve.length
        return ticksSinceStart < durationTicks
    }

    fun timeSinceStart(): Double {
        return ticksSinceStart.toDouble() * DHServer.TICK_LENGTH_SECONDS
    }

    fun step(delta: Double): ShipState {
        return stateForTick(ticksSinceStart++)
    }

    private fun totalDistSoFar(time: Double): Double {
        val startSpeed = startState.velocity.length
        val maxAccel = mainThrust
        val accelTimeSoFar = if(time < timeToFlip) time else timeToFlip
        val decelTimeSoFar = if(time > timeToFlip) time - timeToFlip else 0.0
        val accelTimeTotal = timeToFlip
        val accelDistSoFar = startSpeed * accelTimeSoFar + 0.5 * maxAccel * accelTimeSoFar * accelTimeSoFar
        val accelDistTotal = startSpeed * accelTimeTotal + 0.5 * maxAccel * accelTimeTotal * accelTimeTotal
        //v*v = u*u + 2*a*d
        val speedAtFlip: Double = sqrt(startSpeed * startSpeed + 2 * maxAccel * accelDistTotal)
        //the time it would take to accelerate from end to flip point
        //should be the same as the time it takes to decelerate from flip point to end.
        val decelDistSoFar = speedAtFlip * decelTimeSoFar + 0.5 * -maxAccel * decelTimeSoFar * decelTimeSoFar
        return accelDistSoFar + decelDistSoFar
    }
    //these can only be initialized from zero if starting from zero! breaks recalculation.
    var previousT: Double = 0.0
    var previousPreviousT: Double = 0.0
    var previousPosition: Vector2 = startState.position
    private fun stateForTick(tick: Int): ShipState
    {
        val maxAccel = mainThrust
        val time = tick * DHServer.TICK_LENGTH_SECONDS
        val totalDistSoFar = totalDistSoFar(time)

        val previousTDelta = previousT - previousPreviousT
        val notMoreThan = (previousT + max(0.1, previousTDelta * 5))
        val t = curve.tForDistance(totalDistSoFar, notLessThan = previousT, notMoreThan = notMoreThan)//expensive!
        val newPosition = curve.getCoordinatesAt(t)
        val newVelocity = (newPosition - previousPosition) / DHServer.TICK_LENGTH_SECONDS //this is a cop out.

        val gravity = OrbiterManager.calculateGravityAtTick(0.0, newPosition)
        val gravityCounter = gravity * -1.0
        val tangent = newVelocity.normalized()
        val accelVec = tangent * maxAccel
        val requiredAccel = if(time < timeToFlip) accelVec else accelVec * -1.0
        val totalThrust = requiredAccel + gravityCounter
        val rotation = totalThrust.angle

        previousPreviousT = previousT
        previousT = t
        previousPosition = newPosition
        return ShipState(newPosition, rotation, newVelocity)
    }

    /*fun endTime(assumedDelta: Double): Double
    {
        return startTime + phaseDuration(assumedDelta)
    }*/

    companion object {

        fun travelTimeConstantAccel(startSpeed: Double, accel: Double, distance: Double): Double
        {
            val a = accel
            val d = distance
            val v = startSpeed
            if(a == 0.0){
                if(v == 0.0){
                    return Double.POSITIVE_INFINITY
                } else {
                    return d / v
                }
            } else {
                val sqrtRes = sqrt((2 * a * d) + (v * v))
                val r1 = -1 * ((sqrtRes + v) / a)
                if (r1.isNaN() || r1 < 0) {
                    val r2 = (sqrtRes - v) / a
                    if (r2.isNaN() || r2 < 0) {
                        throw IllegalStateException("No valid result for duration")
                    } else {
                        return r2
                    }
                } else {
                    return r1
                }
            }
        }

        //for a given curve, the flip point is this far along the curve.
        fun distanceFromStartToFlipPoint(startSpeed: Double, endSpeed: Double, accel: Double, curveLength: Double): Double
        {
            return ((accel * curveLength) + (endSpeed - startSpeed)) / (accel * 2)
        }
    }
}