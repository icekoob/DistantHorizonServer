package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.bezier.BezierCurve
import com.dibujaron.distanthorizon.bezier.Order
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState
import java.lang.IllegalStateException
import java.lang.StringBuilder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BezierPhase(startTime: Double, ship: Ship, startState: ShipState, private val targetState: ShipState) :
    NavigationPhase(startTime, startState, ship) {

    //a phase that navigates a smooth curve from startPos with startVel to endPos with endVel
    //makes use of a Bezier Curve.

    private val curve: BezierCurve = BezierCurve.fromStates(startState, targetState, 100)
    private var timeOffsetFromStart = 0.0
    private val duration by lazy { computeDuration() }
    private val timeToFlip by lazy { timeToFlipPoint() }

    override fun phaseDuration(assumedDelta: Double): Double {
        return duration
    }

    override fun getEndState(): ShipState {
        return targetState
    }

    //called by lazy
    fun computeDuration(): Double {
        //in curve space, target vel and start vel are always pointing the same direction
        val endSpeed = targetState.velocity.length
        val startSpeed = startState.velocity.length
        val curveLength = curve.length
        val maxAccel = ship.type.mainThrust
        val accelTime = timeToFlip
        val distToFlipPoint = distanceFromStartToFlipPoint(startSpeed, endSpeed, maxAccel, curveLength)
        val decelDist = curveLength - distToFlipPoint
        //the time it would take to accelerate from end to flip point
        //should be the same as the time it takes to decelerate from flip point to end.
        val decelTime = travelTime(endSpeed, maxAccel, decelDist)
        return accelTime + decelTime
    }

    //called by lazy
    fun timeToFlipPoint(): Double {
        val endSpeed = targetState.velocity.length
        val startSpeed = startState.velocity.length
        val curveLength = curve.length
        val maxAccel = ship.type.mainThrust
        val distToFlipPoint = distanceFromStartToFlipPoint(startSpeed, endSpeed, maxAccel, curveLength)
        val accelDist = distToFlipPoint
        val accelTime = travelTime(startSpeed, maxAccel, accelDist)
        return accelTime
    }


    override fun hasNextStep(delta: Double): Boolean {
        val newT = timeOffsetFromStart + delta
        return newT <= duration
    }

    override fun step(delta: Double): ShipState {
        val newT = timeOffsetFromStart + delta
        val state = stateAtTime(newT, delta)
        timeOffsetFromStart = newT
        return state
    }

    var previousT: Double = 0.0
    var previousPreviousT: Double = 0.0
    var diagnosticBuilder = StringBuilder()
    private fun stateAtTime(time: Double, timeDelta: Double): ShipState
    {
        diagnosticBuilder = StringBuilder()
        val t1 = System.currentTimeMillis()
        val startSpeed = startState.velocity.length
        val maxAccel = ship.type.mainThrust
        val accelTime = if(time < timeToFlip) time else timeToFlip
        val decelTime = if(time > timeToFlip) time - timeToFlip else 0.0
        val accelDist = startSpeed * accelTime + 0.5 * maxAccel * accelTime * accelTime

        //v*v = u*u + 2*a*d
        val speedAtFlip: Double = sqrt(startSpeed * startSpeed + 2 * maxAccel * accelDist)
        //the time it would take to accelerate from end to flip point
        //should be the same as the time it takes to decelerate from flip point to end.
        val decelDist = speedAtFlip * decelTime + 0.5 * -maxAccel * decelTime * decelTime
        val totalDist = accelDist + decelDist

        val previousTDelta = previousT - previousPreviousT
        val t2 = System.currentTimeMillis()
        diagnosticBuilder.append("preparation=${t2-t1} ")
        val t = curve.tForDistance(totalDist, notLessThan = previousT, notMoreThan = (previousT + max(0.1, previousTDelta * 5)) )//expensive!
        val t3 = System.currentTimeMillis()
        diagnosticBuilder.append("tForDistance=${t3-t2} ")
        val newPosition = curve.getCoordinatesAt(t)
        val t4 = System.currentTimeMillis()
        diagnosticBuilder.append("getCoordsAt=${t4-t3} ")
        val newVelocity = (newPosition - ship.currentState.position) * timeDelta //this is a cop out.

        val gravity = OrbiterManager.calculateGravity(0.0, newPosition)
        val t5 = System.currentTimeMillis()
        diagnosticBuilder.append("gravity=${t5-t4}")
        val gravityCounter = gravity * -1.0
        val tangent = newVelocity.normalized()
        val accelVec = tangent * maxAccel
        val requiredAccel = if(time < timeToFlip) accelVec else accelVec * -1.0
        val totalThrust = requiredAccel + gravityCounter
        val rotation = totalThrust.angle

        previousPreviousT = previousT
        previousT = t
        return ShipState(newPosition, rotation, newVelocity)
    }

    companion object {

        fun travelTime(startSpeed: Double, accel: Double, distance: Double): Double
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