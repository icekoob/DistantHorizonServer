package com.dibujaron.distanthorizon.navigation

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState

class StationAdjacentRotationPhase(
    ship: Ship,
    startTime: Double,
    startState: ShipState,
    val station: Station,
    endRotation: Double
) : AbstractRotationPhase(startTime, ship, startState, endRotation) {

    val relativePosition: Vector2 by lazy {
        (station.globalPosAtTime(startTime) - startState.position).rotated(-1 * station.globalRotationAtTime(startTime))
    }
    //a navigation phase that orbits the given body from startAngle at Altitude through angularDist (could be negative)
    //StableOrbit class may help with this.


    var currentTime = startTime
    override fun step(delta: Double): ShipState {
        val newTime = currentTime + delta
        val rotation = stepRotation(delta)
        val relPosRotated = relativePosition.rotated(station.globalRotationAtTime(newTime))
        val position = station.globalPosAtTime(newTime) + relPosRotated
        val velocity = station.velocityAtTime(newTime)
        currentTime = newTime
        return ShipState(position, rotation, velocity)
    }
}