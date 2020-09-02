package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.bezier.BezierCurve
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.ShipState
import java.lang.IllegalStateException
import kotlin.math.*

@Deprecated("use BezierNavigationPhase instead")
class BezierPhase(val mainEngineThrust: Double, val startState: ShipState, private val targetState: ShipState){

    //a phase that navigates a smooth curve from startPos with startVel to endPos with endVel
    //makes use of a Bezier Curve.

    val curve: BezierCurve = BezierCurve.fromStates(startState, targetState, 1000)
    var ticksSinceStart = 0
    val tickToFlip: Int by lazy { ticksToFlipPoint() }
    val durationTicks by lazy {computeDurationTicks() }
    val durationTicksInt: Int by lazy {ceil(durationTicks).toInt()}
    val accelPerTick = mainEngineThrust / (DHServer.TICKS_PER_SECOND * DHServer.TICKS_PER_SECOND)
    fun getEndState(): ShipState {
        return targetState
    }

    //called by lazy
    fun computeDurationTicks(): Double {
        //in curve space, target vel and start vel are always pointing the same direction
        val endSpeedTicks = targetState.velocityTicks.length
        val startSpeedTicks = startState.velocityTicks.length
        val curveLength = curve.length
        val accelTicks = tickToFlip
        val distToFlipPoint = distanceFromStartToFlipPoint(startSpeedTicks, endSpeedTicks, accelPerTick, curveLength)
        val decelDist = curveLength - distToFlipPoint
        //the time it would take to accelerate from end to flip point
        //should be the same as the time it takes to decelerate from flip point to end.
        val decelTicks = travelTimeConstantAccel(endSpeedTicks, accelPerTick, decelDist)
        return accelTicks + decelTicks
    }

    //called by lazy
    private fun ticksToFlipPoint(): Int {
        val endSpeedTicks = targetState.velocityTicks.length
        val startSpeedTicks = startState.velocityTicks.length
        val curveLength = curve.length
        val maxAccel = accelPerTick
        val distToFlipPoint = distanceFromStartToFlipPoint(startSpeedTicks, endSpeedTicks, accelPerTick, curveLength)
        return travelTimeConstantAccel(startSpeedTicks, maxAccel, distToFlipPoint).roundToInt()
    }


    fun hasNextStep(delta: Double): Boolean {
        return totalDistSoFar(ticksSinceStart) <= curve.length
        //return ticksSinceStart < durationTicksInt
    }

    fun step(delta: Double): ShipState {
        return stateForTick(ticksSinceStart++)
    }

    private fun totalDistSoFar(tick: Int): Double {
        val startSpeedTicks = startState.velocityTicks.length
        val accelTicksSoFar = if(tick < tickToFlip) tick else tickToFlip
        val decelTicksSoFar = if(tick > tickToFlip) tick - tickToFlip else 0
        val accelTimeTotal = tickToFlip
        val accelDistSoFar = startSpeedTicks * accelTicksSoFar + 0.5 * accelPerTick * accelTicksSoFar * accelTicksSoFar
        val accelDistTotal = startSpeedTicks * accelTimeTotal + 0.5 * accelPerTick * accelTimeTotal * accelTimeTotal
        //v*v = u*u + 2*a*d
        val speedAtFlip: Double = sqrt(startSpeedTicks * startSpeedTicks + 2 * accelPerTick * accelDistTotal)
        //the time it would take to accelerate from end to flip point
        //should be the same as the time it takes to decelerate from flip point to end.
        val decelDistSoFar = speedAtFlip * decelTicksSoFar + 0.5 * -accelPerTick * decelTicksSoFar * decelTicksSoFar
        return accelDistSoFar + decelDistSoFar
    }


    //these can only be initialized from zero if starting from zero! breaks recalculation.
    var previousT: Double = 0.0
    var previousPreviousT: Double = 0.0
    var previousPosition: Vector2 = startState.position
    private fun stateForTick(tick: Int): ShipState
    {
        val totalDistSoFar = totalDistSoFar(tick)

        val previousTDelta = previousT - previousPreviousT
        val notMoreThan = (previousT + max(0.1, previousTDelta * 5))
        val t = curve.tForDistance(totalDistSoFar, notLessThan = previousT, notMoreThan = notMoreThan)//expensive!
        val newPosition = curve.getCoordinatesAt(t)
        val newVelocity = (newPosition - previousPosition) / DHServer.TICK_LENGTH_SECONDS //convert back to seconds

        val gravity = OrbiterManager.calculateGravityAtTick(0.0, newPosition)
        val gravityCounter = gravity * -1.0
        val tangent = newVelocity.normalized()
        val accelVec = tangent * accelPerTick
        val requiredAccel = if(tick < tickToFlip) accelVec else accelVec * -1.0
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
                val r2 = (sqrtRes - v) / a
                if (r1.isNaN() || r1 < 0) {
                    if (r2.isNaN() || r2 < 0) {
                        throw IllegalStateException("No valid result for duration")
                    } else {
                        return r2
                    }
                } else if(r2.isNaN() || r2 < 0){
                    return r1
                } else {
                    return min(r1, r2)
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