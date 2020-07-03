package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.navigation.BezierNavigationPhase
import com.dibujaron.distanthorizon.navigation.BezierPhase
import com.dibujaron.distanthorizon.navigation.NavigationRoute
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipClassManager
import com.dibujaron.distanthorizon.ship.ShipColor
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.controller.AIShipController
import org.junit.BeforeClass
import org.junit.Test
import java.awt.Color
import kotlin.math.abs

class OrbiterTest {

    @Test
    fun testStationDrift() {
        DHServer.timer.cancel()
        val station = OrbiterManager.getStations().asSequence().first()
        val expectedPosition = station.globalPosAtTick(10000.0)
        repeat(10000) {
            OrbiterManager.process(DHServer.TICK_LENGTH_SECONDS)
        }
        val truePosition = station.globalPos()
        val positionError = abs((expectedPosition - truePosition).length)
        println("error: $positionError")
        assert(positionError < 1)
    }

    @Test
    fun testStationDriftABunchOfTimes(){
        repeat(10){testStationDrift()}
    }

    @Test
    fun testDockingPortDrift() {
        DHServer.timer.cancel()
        val station = OrbiterManager.getStations().asSequence().first()
        val port = station.dockingPorts[0]
        val expectedPosition = port.globalPosAtTick(10000.0)
        repeat(10000) {
            OrbiterManager.process(DHServer.TICK_LENGTH_SECONDS)
        }
        val truePosition = port.globalPosition()
        val positionError = abs((expectedPosition - truePosition).length)
        println("error: $positionError")
        assert(positionError < 1)
    }

    @Test
    fun testDockingPortDriftWithPhase(){
        DHServer.timer.cancel()
        val station = OrbiterManager.getStations().asSequence().first()
        val port = station.dockingPorts[0]

        val ship = Ship(
            ShipClassManager.getShipClass(DHServer.playerStartingShip)!!,
            ShipColor(Color.WHITE),
            ShipColor(Color.WHITE),
            ShipState(Vector2.ZERO, 0.0, Vector2(10, 0)),
            AIShipController()
        )

        val shipPort = ship.myDockingPorts[0]
        var expectedPosition = Vector2.ZERO
        val phase = NavigationRoute.trainPhase(0){ endTickEst ->
            val endVel = port.velocityAtTick(endTickEst)
            val endPortGlobalPos = port.globalPosAtTick(endTickEst)
            expectedPosition = endPortGlobalPos
            val endRotation = port.globalRotationAtTick(endTickEst) + shipPort.relativeRotation() //why do I keep having to offset by pi here
            val targetPos = endPortGlobalPos + (shipPort.relativePosition() * -1.0).rotated(endRotation)
            val endState = ShipState(targetPos, endRotation, endVel)
            //lastEstEndTime = endTimeEst
            BezierNavigationPhase(ship.type.mainThrust, ship.currentState, endState)
        }
        val expectedIterations = phase.durationTicks.toInt() + 1
        var iterations = 0
        while(phase.hasNextStep()){
            phase.step()
            OrbiterManager.process(DHServer.TICK_LENGTH_SECONDS)
            iterations++
        }
        assert(iterations == expectedIterations)

        val truePosition = port.globalPosition()
        val positionError = (expectedPosition - truePosition).length
        println("error: $positionError")
        assert(positionError < 1)
    }

    @Test
    fun testDockingPortDriftWithRepeat(){
        DHServer.timer.cancel()
        val station = OrbiterManager.getStations().asSequence().first()
        val port = station.dockingPorts[0]

        val ship = Ship(
            ShipClassManager.getShipClass(DHServer.playerStartingShip)!!,
            ShipColor(Color.WHITE),
            ShipColor(Color.WHITE),
            ShipState(Vector2.ZERO, 0.0, Vector2(10, 0)),
            AIShipController()
        )

        val shipPort = ship.myDockingPorts[0]
        var expectedPosition = Vector2.ZERO
        val phase = NavigationRoute.trainPhase(0){ endTickEst ->
            val endVel = port.velocityAtTick(endTickEst)
            val endPortGlobalPos = port.globalPosAtTick(endTickEst)
            expectedPosition = endPortGlobalPos
            val endRotation = port.globalRotationAtTick(endTickEst) + shipPort.relativeRotation() //why do I keep having to offset by pi here
            val targetPos = endPortGlobalPos + (shipPort.relativePosition() * -1.0).rotated(endRotation)
            val endState = ShipState(targetPos, endRotation, endVel)
            //lastEstEndTime = endTimeEst
            BezierNavigationPhase(ship.type.mainThrust, ship.currentState, endState)
        }

        repeat(phase.durationTicks.toInt()) {
            phase.step()
            OrbiterManager.process(DHServer.TICK_LENGTH_SECONDS)
        }

        val truePosition = port.globalPosition()
        val positionError = (expectedPosition - truePosition).length
        println("error: $positionError")
        assert(positionError < 1)
    }
}