package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState
import dev.benedikt.math.bezier.curve.BezierCurve
import dev.benedikt.math.bezier.curve.DoubleBezierCurve
import dev.benedikt.math.bezier.curve.Order
import dev.benedikt.math.bezier.vector.Vector2D
import java.lang.IllegalStateException
import kotlin.math.abs
import kotlin.math.sqrt

class BezierPhase(startTime: Double, ship: Ship, startState: ShipState, val targetState: ShipState) :
    NavigationPhase(startTime, startState, ship) {

    //a phase that navigates a smooth curve from startPos with startVel to endPos with endVel
    //makes use of a Bezier Curve.

    private val curve: BezierCurve<Double, Vector2D> = initCurve()
    private var timeOffsetFromStart = 0.0
    private val duration by lazy{computeDuration()}

    private fun initCurve(): BezierCurve<Double, Vector2D> {
        val c1: Vector2D = startState.position.toBezierVector()
        val c2: Vector2D = (startState.position + startState.velocity).toBezierVector()
        val c3: Vector2D = (targetState.position - targetState.velocity).toBezierVector()
        val c4: Vector2D = targetState.position.toBezierVector()
        val controlPoints = arrayListOf(c2, c3)
        val curve = DoubleBezierCurve(Order.CUBIC, c1, c4, controlPoints)
        curve.computeLength()
        return curve
    }

    override fun phaseDuration(assumedDelta: Double): Double {
        return duration
    }

    override fun getEndState(): ShipState {
        return targetState
    }
    //called by lazy
    fun computeDuration(): Double {
        val a = abs(targetState.velocity.length - startState.velocity.length)
        val u = startState.velocity.length
        val s = curve.length
        if(a == 0.0){
            if(u != 0.0){
                return s / u
            } else {
                return Double.POSITIVE_INFINITY
            }
        } else {
            val sqrtRes = sqrt((2 * a * s) + (u * u))
            val r1 = -1 * ((sqrtRes + u) / a)
            if(r1.isNaN() || r1 < 0){
                val r2 = (sqrtRes - u) / a
                if(r2.isNaN() || r2 < 0){
                    throw IllegalStateException("No valid result for duration")
                } else {
                    return r2
                }
            } else {
                return r1
            }
        }
    }

    override fun hasNextStep(delta: Double): Boolean {
        val newT = tForTimeOffset(timeOffsetFromStart + delta)
        return newT <= 1.0
    }

    override fun step(delta: Double): ShipState {
        val newTime = timeOffsetFromStart + delta
        val newT = tForTimeOffset(newTime)
        val pos = Vector2(curve.getCoordinatesAt(newT))
        val futureT = newT + tForTimeOffset(0.01)
        val futurePos = Vector2(curve.getCoordinatesAt(futureT))
        val velocity = (futurePos - pos) * 100.0
        val pastT = newT + tForTimeOffset(-0.01)
        val pastPos = Vector2(curve.getCoordinatesAt(pastT))
        val pastVelocity = (pos - pastPos) * 100.0
        val requiredAccelForFiftiethOfSecond = (velocity - pastVelocity)
        val requiredAccel = requiredAccelForFiftiethOfSecond * 50.0
        val gravity = OrbiterManager.calculateGravity(0.0, pos)
        val gravityCounter = gravity * -1.0
        val totalThrust = requiredAccel + gravityCounter
        val rotation = totalThrust.angle
        timeOffsetFromStart = newTime
        return ShipState(pos, rotation + Math.PI / 2, velocity)
    }

    fun tForTimeOffset(timeOffset: Double): Double {
        return timeOffset / duration
    }
}