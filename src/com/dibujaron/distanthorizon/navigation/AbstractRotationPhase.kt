package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.AngleUtils
import com.dibujaron.distanthorizon.ship.Ship
import kotlin.math.ceil

abstract class AbstractRotationPhase(startTime: Double, ship: Ship, startState: NavigationState, endRotation: Double) :
    NavigationPhase(startTime, startState, ship) {

    //a navigation phase that rotates the ship through the given theta.
    //not a complete phase because this does not supply position or velocity.

    var rotationCurrent = startState.rotation
    var thetaRemaining = AngleUtils.angularDiff(startState.rotation, endRotation)

    override fun hasNextStep(delta: Double): Boolean {
        return thetaRemaining != 0.0
    }

    //step is not overridden
    fun stepRotation(delta: Double): Double {
        val maxRot = ship.type.rotationPower * delta
        if (thetaRemaining > maxRot) {
            rotationCurrent += maxRot
            thetaRemaining -= maxRot
        } else if (thetaRemaining > 0.0) {
            rotationCurrent += thetaRemaining
            thetaRemaining = 0.0
        } else if (thetaRemaining < -maxRot) {
            rotationCurrent -= maxRot
            thetaRemaining += maxRot
        } else if (thetaRemaining < 0.0) {
            rotationCurrent -= thetaRemaining
            thetaRemaining = 0.0
        }
        return rotationCurrent
    }

    override fun phaseDuration(assumedDelta: Double): Double {
        val ticksRequired = ceil(thetaRemaining / (ship.type.rotationPower * assumedDelta)).toInt()
        return ticksRequired * assumedDelta
    }
}