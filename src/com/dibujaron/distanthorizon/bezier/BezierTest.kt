package com.dibujaron.distanthorizon.bezier

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.ShipState
import org.junit.Test

class BezierTest {

    @Test
    fun testTForDistance(){
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val endState = ShipState(Vector2(1000,1000), 0.0, Vector2(0, 10))
        val curve = BezierCurve.fromStates(startState, endState, 100)
        val length = curve.length
        println("length is ${curve.length}")
        val tForDist = curve.tForDistance(1000.0, 0.0, 1.0,true)
        println("t for 1000 is $tForDist")
    }

    @Test
    fun testTForLongDistance(){
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val endState = ShipState(Vector2(1000000,1000000), 0.0, Vector2(0, 10))
        val curve = BezierCurve.fromStates(startState, endState, 100)
        val length = curve.length
        println("length is ${curve.length}")
        val tForDist = curve.tForDistance(1000.0, 0.0,1.0,true)
        println("t for 1000 is $tForDist")
    }

    @Test
    fun testTForLotsOfDistances(){
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val endState = ShipState(Vector2(1000000,1000000), 0.0, Vector2(0, 10))
        val curve = BezierCurve.fromStates(startState, endState, 100)
        val length = curve.length
        val t = System.currentTimeMillis()
        val n = 100000
        (0..n).asSequence().map{Math.random() * length}.forEach {
            curve.tForDistance(it)
        }
        val time = System.currentTimeMillis() - t
        println("Completed $n calculations in ${time}ms, approx ${(time * 1.0) / n}ms per calculation")
    }
}