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
        while(phase.hasNextStep()){
            phase.step()
            OrbiterManager.process(DHServer.TICK_LENGTH_SECONDS)
        }

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

    @Test
    fun testDockingPositionDrift(){
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
        var expectedDockingPosition = Vector2.ZERO
        val phase = NavigationRoute.trainPhase(0){ endTickEst ->
            val endVel = port.velocityAtTick(endTickEst)
            val endPortGlobalPos = port.globalPosAtTick(endTickEst)
            val endRotation = port.globalRotationAtTick(endTickEst) + shipPort.relativeRotation() //why do I keep having to offset by pi here
            val targetPos = endPortGlobalPos + (shipPort.relativePosition() * -1.0).rotated(endRotation)
            expectedDockingPosition = targetPos
            val endState = ShipState(targetPos, endRotation, endVel)
            //lastEstEndTime = endTimeEst
            BezierNavigationPhase(ship.type.mainThrust, ship.currentState, endState)
        }

        var result = phase.startState
        repeat(phase.durationTicks.toInt()) {
            result = phase.step()
            OrbiterManager.process(DHServer.TICK_LENGTH_SECONDS)
        }

        val positionError = (result.position - expectedDockingPosition).length
        println("error: $positionError")
        assert(positionError < 1)
    }

    @Test
    fun testKnownPositionDrift(){
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

        val startState = ShipState(Vector2(-118.23776432029061,1908.6427847005539), 0.7404478346016786, Vector2(-68.26590446020333,-149.55021296808582))
        val endState = ShipState(Vector2(-444.6872120090582,5767.165063624374), -5.97540415928624, Vector2(-205.5312943091849,-131.17748068816582))
        val phase = BezierNavigationPhase(120.0, startState, endState)

        var result = phase.startState
        while(phase.hasNextStep()){
            result = phase.step()
        }

        val positionError = (result.position - endState.position).length
        println("error: $positionError")
        assert(positionError < 1)
    }
}