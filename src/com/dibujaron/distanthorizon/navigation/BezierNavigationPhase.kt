package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.bezier.BezierCurve
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.ShipState
import java.lang.IllegalStateException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BezierNavigationPhase(val mainEngineThrust: Double, val startState: ShipState, val endState: ShipState)
{
    val curve: BezierCurve = BezierCurve.fromStates(startState, endState, 100)
    val accelPerTick: Double = mainEngineThrust / (DHServer.TICKS_PER_SECOND * DHServer.TICKS_PER_SECOND)
    val startSpeedTicks = startState.velocityTicks.length
    val endSpeedTicks = endState.velocityTicks.length

    //flip point information
    val flipPointDistance = ((accelPerTick * curve.length) + (endSpeedTicks - startSpeedTicks)) / (accelPerTick * 2)
    //val flipPointDistanceNew = ((2 * accelPerTick * curve.length) - (startSpeedTicks*startSpeedTicks) + (endSpeedTicks*endSpeedTicks)) / (accelPerTick * 4)
    val flipTick = distanceToTick(flipPointDistance)
    val speedAtFlip = sqrt((startSpeedTicks * startSpeedTicks) + (2 * accelPerTick * flipPointDistance))

    var previousT: Double = 0.0
    var previousPosition: Vector2 = startState.position

    //duration
    val durationTicks = distanceToTick(curve.length)
    var ticksSinceStart = 0
    fun hasNextStep(): Boolean{
        return ticksSinceStart < durationTicks
        //return previousT < 0.999999
    }

    fun step(): ShipState
    {
        return stateForTick(ticksSinceStart++)
    }

    private fun stateForTick(tick: Int): ShipState
    {
        val distance = tickToDistance(tick.toDouble())
        val t = curve.tForDistance(distance, previousT, 1.0)
        val position = curve.getCoordinatesAt(t)
        val velocity = (position - previousPosition) / DHServer.TICK_LENGTH_SECONDS //convert back to seconds

        //this is rotation.
        val gravity = OrbiterManager.calculateGravityAtTick(0.0, position)
        val gravityCounter = gravity * -1.0
        val tangent = velocity.normalized()
        val accelVec = tangent * accelPerTick
        val requiredAccel = if(tick < flipTick) accelVec else accelVec * -1.0
        val totalThrust = requiredAccel + gravityCounter
        val rotation = totalThrust.angle

        previousT = t
        previousPosition = position
        return ShipState(position, rotation, velocity)
    }


    fun tickToDistance(tick: Double): Double
    {
        val accelTicksSoFar = if(tick < flipTick) tick else flipTick
        val decelTicksSoFar = if(tick > flipTick) tick - flipTick else 0.0
        //d = vt + (at^2 / 2)
        val accelDistance = startSpeedTicks * accelTicksSoFar + ((accelPerTick * accelTicksSoFar * accelTicksSoFar) / 2)
        val decelDistance = speedAtFlip * decelTicksSoFar + ((-1 * accelPerTick * decelTicksSoFar * decelTicksSoFar) / 2)
        return accelDistance + decelDistance
    }

    fun distanceToTick(distance: Double): Double
    {
        val accelDistanceSoFar = if(distance < flipPointDistance) distance else flipPointDistance
        val decelDistanceSoFar = if(distance > flipPointDistance) distance - flipPointDistance else 0.0
        val accelTimeSoFar = timeFromInitialVelocityAndAcceleration(accelDistanceSoFar, startSpeedTicks, accelPerTick)
        val decelTimeSoFar = timeFromInitialVelocityAndAcceleration(decelDistanceSoFar, speedAtFlip, -accelPerTick)
        return accelTimeSoFar + decelTimeSoFar
    }

    companion object {
        fun timeFromInitialVelocityAndAcceleration(displacement: Double, initialVelocity: Double, acceleration: Double): Double {
            if(acceleration == 0.0){
                return if(initialVelocity == 0.0){
                    Double.POSITIVE_INFINITY
                } else {
                    displacement / initialVelocity
                }
            } else {
                val rootResult = sqrt((2 * acceleration * displacement) + (initialVelocity * initialVelocity))
                val r1 = -1 * ((rootResult + initialVelocity) / acceleration)
                val r2 = (rootResult - initialVelocity) / acceleration
                if(r1.isNaN() || r1 < 0){
                    return r2
                } else if(r2.isNaN() || r2 < 0){
                    return r1
                } else {
                    return min(r1, r2)
                }
            }
        }
    }
}