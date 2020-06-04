package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState

class StationAdjacentWaitingPhase(
    ship: Ship,
    startTime: Double,
    startState: ShipState,
    val station: Station,
    val waitTime: Double
) : NavigationPhase(startTime, startState, ship) {

    val relativePosition: Vector2 by lazy {
        (startState.position - station.globalPosAtTime(startTime)).rotated(-1 * station.globalRotationAtTime(startTime))
    }
    //a navigation phase that orbits the given body from startAngle at Altitude through angularDist (could be negative)
    //StableOrbit class may help with this.

    var currentTime = startTime

    override fun hasNextStep(delta: Double): Boolean {
        return currentTime < startTime + waitTime
    }
    //problem may be that we're working adjacent to the station, not the docking port.
    override fun step(delta: Double): ShipState {
        val newTime = currentTime + delta
        val rotation = station.globalRotationAtTime(newTime)
        val relPosRotated = relativePosition.rotated(station.globalRotationAtTime(newTime))
        val position = station.globalPosAtTime(newTime) + relPosRotated
        val velocity = station.velocityAtTime(newTime)
        currentTime = newTime
        return ShipState(position, rotation, velocity)
    }

    override fun phaseDuration(assumedDelta: Double): Double {
        return waitTime
    }
}