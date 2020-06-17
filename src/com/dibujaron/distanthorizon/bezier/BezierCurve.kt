package com.dibujaron.distanthorizon.bezier

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.ShipState
import java.lang.IllegalStateException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow

//sourced originally from https://github.com/Bw2801/bezier-spline-kotlin and modified
class BezierCurve(val order: Order, val from: Vector2, val to: Vector2, val controlPoints: Collection<Vector2>,
                       val resolution: Int ){

    val MIN_MEANINGFUL_DIFFERENCE_IN_T = 2.0.pow(-10)
    val knots = this.from to this.to
    val points: List<Vector2>

    //this is a mapping from [0..resolution] to the length
    //e.g. lengthCache[x] = distance from start to t(x/resolution)
    val coordinateLengthCache: ArrayList<VectorWithDistance>

    val length: Double

    init {
        if (this.controlPoints.size != this.order.controlPoints) {
            throw IllegalArgumentException("The bezier curve of order ${order.name} expects exactly ${order.controlPoints} control points.")
        }

        if (this.order.degree < Order.LINEAR.degree || this.order.previous == null) {
            throw IllegalArgumentException("The bezier curve expects a minimum order of ${Order.LINEAR.name}.")
        }

        val points = mutableListOf(this.from)
        points.addAll(this.controlPoints)
        points.add(to)
        this.points = points.toList()
        coordinateLengthCache = ArrayList(this.resolution + 1)

        //compute the length.
        val fraction = 1.0 / this.resolution
        var length = 0.0
        var lastCoordinates = this.from
        coordinateLengthCache.add(VectorWithDistance(this.from,0.0))
        for (i in 1..this.resolution) {
            val coordinates = this.getCoordinatesAt(fraction * i)
            length += (lastCoordinates - coordinates).length
            coordinateLengthCache.add(VectorWithDistance(coordinates, length))
            lastCoordinates = coordinates
        }

        this.length = length
    }

    fun getCoordinatesAt(t: Double) : Vector2 {
        return this.getCoordinatesAt(t, this.order, this.points)
    }
    private fun getCoordinatesAt(t: Double, order: Order, points: List<Vector2>) : Vector2 {
        var result : Vector2? = null

        val mt = 1.0 - t
        val size = order.binomals.size

        for (i in 0 until size) {
            val part = points[i] * (mt.pow(size - i - 1) * t.pow(i) * order.binomals[i])
            result = if (result == null) part else result + part
        }

        return result!!
    }
    fun distanceForT(t: Double): Double
    {
        val lower = (t * this.resolution).toInt() //this is the lower
        val cached = coordinateLengthCache[lower]
        val closestCached = cached.position
        val posForT = getCoordinatesAt(t)
        val distanceFromClosest = (posForT - closestCached).length
        return cached.distanceFromStart + distanceFromClosest
    }
    fun tForDistance(distanceFromStart: Double, notLessThan: Double = 0.0, notMoreThan: Double = 1.0, debug: Boolean = false): Double{
        var lowerLimit = if(notLessThan > 0.0) notLessThan else 0.0
        var upperLimit = if(notMoreThan < 1.0) notMoreThan else 1.0
        var i = 0
        var priorM = 0.0
        val t = System.currentTimeMillis()
        while(lowerLimit < upperLimit && ((upperLimit - lowerLimit) > MIN_MEANINGFUL_DIFFERENCE_IN_T)){
            val m = (lowerLimit + upperLimit) / 2.0
            if(m == priorM){
                if(debug) println("got repeated m in $i iterations")
                return m
            }
            val distanceForM = distanceForT(m)
            //println("$m, $distanceForM")
            if(distanceForM < distanceFromStart){
                lowerLimit = m
            } else if (distanceForM > distanceFromStart){
                upperLimit = m
            } else {
                if(debug) println("converged in $i iterations")
                return m
            }
            priorM = m
            i++
        }
        val diff = System.currentTimeMillis() - t
        if(diff > 1 && DHServer.debug){
            println("Bezier next state took $diff ms, i=$i")
        }
        if(debug) println("lower limit is same as upper limit after $i iterations")
        return lowerLimit
    }

    companion object {
        class VectorWithDistance(val position: Vector2, val distanceFromStart: Double)
        fun fromStates(startState: ShipState, targetState: ShipState, resolution: Int): BezierCurve{
            val c1: Vector2 = startState.position
            val c2: Vector2 = (startState.position + startState.velocity)
            val c3: Vector2 = (targetState.position - targetState.velocity)
            val c4: Vector2 = targetState.position
            val controlPoints = arrayListOf(c2, c3)
            return BezierCurve(Order.CUBIC, c1, c4, controlPoints, resolution)
        }
    }
}