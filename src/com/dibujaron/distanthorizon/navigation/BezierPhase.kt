package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.Ship
import dev.benedikt.math.bezier.curve.BezierCurve
import dev.benedikt.math.bezier.curve.DoubleBezierCurve
import dev.benedikt.math.bezier.curve.Order
import dev.benedikt.math.bezier.vector.Vector2D

class BezierPhase(ship: Ship, val startPos: Vector2, val startVel: Vector2, val endPos: Vector2, val endVel: Vector2) :
    NavigationPhase(ship) {

    //a phase that navigates a smooth curve from startPos with startVel to endPos with endVel
    //makes use of a Bezier Curve.

    private val curve: BezierCurve<Double, Vector2D> = initCurve()
    private var currentT = 0.0
    private fun initCurve(): BezierCurve<Double, Vector2D> {
        val c1: Vector2D = startPos.toBezierVector()
        val c2: Vector2D = (startPos + startVel).toBezierVector()
        val c3: Vector2D = (endPos + endVel).toBezierVector()
        val c4: Vector2D = endPos.toBezierVector()
        val controlPoints = arrayListOf(c1, c2, c3, c4)
        val curve = DoubleBezierCurve(Order.CUBIC, c1, c3, controlPoints)
        curve.computeLength()
        return curve
    }

    override fun hasNextStep(delta: Double): Boolean {
        //todo
        return false;
    }

    override fun step(delta: Double): NavigationStep {
        //todo
        return NavigationStep(0.0, Vector2(0.0, 0.0), Vector2(0.0, 0.0))
    }

    //we want to smoothly accelerate along the curve.
    fun velocityAtT(t: Double): Vector2 {
        val deltaV = endVel.length - startVel.length
        val changeByT = deltaV * t
        val speed = startVel.length + changeByT
        return Vector2(curve.getTangentAt(t)) * speed
    }

    fun tForDistance(dist: Double): Double {
        return dist / curve.length
    }
}