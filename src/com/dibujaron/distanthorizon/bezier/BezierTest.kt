package com.dibujaron.distanthorizon.bezier

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.navigation.BezierNavigationPhase
import com.dibujaron.distanthorizon.navigation.BezierPhase
import com.dibujaron.distanthorizon.navigation.NavigationRoute
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.ship.*
import com.dibujaron.distanthorizon.ship.controller.AIShipController
import org.junit.Test
import java.awt.Color
import kotlin.math.abs

class BezierTest {

    @Test
    fun testTForDistance(){
        DHServer.timer.cancel()
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val endState = ShipState(Vector2(1000,1000), 0.0, Vector2(0, 10))
        val curve = BezierCurve.fromStates(startState, endState, 100)
        println("length is ${curve.length}")
        val tForDist = curve.tForDistance(1000.0, 0.0, 1.0,true)
        println("t for 1000 is $tForDist")
    }

    @Test
    fun testTForLongDistance(){
        DHServer.timer.cancel()
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val endState = ShipState(Vector2(1000000,1000000), 0.0, Vector2(0, 10))
        val curve = BezierCurve.fromStates(startState, endState, 100)
        println("length is ${curve.length}")
        val tForDist = curve.tForDistance(1000.0, 0.0,1.0,true)
        println("t for 1000 is $tForDist")
    }

    @Test
    fun testTForLotsOfDistances(){
        DHServer.timer.cancel()
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

    @Test
    fun testTForLengthIs1(){
        DHServer.timer.cancel()
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val endState = ShipState(Vector2(1000000,1000000), 0.0, Vector2(0, 10))
        val curve = BezierCurve.fromStates(startState, endState, 100)
        val tForDist = curve.tForDistance(curve.length, 0.0,1.0,true)
        assert(tForDist > 0.99)
    }

    @Test
    fun testArriveAtDest(){
        DHServer.timer.cancel()
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val endState = ShipState(Vector2(1000000,1000000), 0.0, Vector2(0, 10))
        val phase = BezierPhase(60.0, startState, endState)
        var output: ShipState = startState
        var nextTickLength = DHServer.TICK_LENGTH_SECONDS + (Math.random() * 0.5) - 0.25
        while(phase.hasNextStep(nextTickLength)){
            output = phase.step(nextTickLength)
            nextTickLength = DHServer.TICK_LENGTH_SECONDS + (Math.random() * 0.5) - 0.25
        }
        assert(phase.previousT > 0.99)
        assert((endState.position - output.position).length < 10)
        assert(abs(phase.durationTicks - phase.ticksSinceStart) < 1)
    }

    @Test
    fun testArriveAtStation(){
        DHServer.timer.cancel()
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val destinationStation = OrbiterManager.getStations().asSequence().first()
        val destinationPort = destinationStation.dockingPorts[0]
        val ship = Ship(
            ShipClassManager.getShipClass(DHServer.playerStartingShip)!!,
            ShipColor(Color.WHITE),
            ShipColor(Color.WHITE),
            startState,
            AIShipController()
        )
        var targetPortPositionFromPhase = Vector2.ZERO
        val shipPort = ship.myDockingPorts[0]
        val phase = NavigationRoute.trainPhase(0){ endTickEst ->
            val endVel = destinationPort.velocityAtTick(endTickEst)
            val endPortGlobalPos = destinationPort.globalPosAtTick(endTickEst)
            targetPortPositionFromPhase = endPortGlobalPos
            val endRotation = destinationPort.globalRotationAtTick(endTickEst) + shipPort.relativeRotation() //why do I keep having to offset by pi here
            val targetPos = endPortGlobalPos + (shipPort.relativePosition() * -1.0).rotated(endRotation)
            val endState = ShipState(targetPos, endRotation, endVel)
            //lastEstEndTime = endTimeEst
            BezierNavigationPhase(ship.type.mainThrust, ship.currentState, endState)
        }

        val phaseEndState = phase.endState

        var output: ShipState = startState
        val expectedIterations = phase.durationTicks.toInt() + 1
        var iterations = 0
        val duration = phase.durationTicks
        val targetPortPosIndependent = destinationPort.globalPosAtTick(duration)
        while(phase.hasNextStep()){
            output = phase.step()
            OrbiterManager.process(DHServer.TICK_LENGTH_SECONDS)
            iterations++
        }

        val targetPortTruePosition = destinationPort.globalPosition()
        val finalT = phase.previousT
        val targetError = (phaseEndState.position - output.position).length
        val trueError = (targetPortTruePosition - output.position).length

        val endPortGlobalPos = destinationPort.globalPosition()

        val errorTrueFromPhase = (targetPortTruePosition - targetPortPositionFromPhase).length
        val errorTrueFromIndependent = (targetPortTruePosition - targetPortPosIndependent).length
        val errorIndependentFromPhase = (targetPortPosIndependent - targetPortPositionFromPhase).length
        assert(errorTrueFromPhase < 1)
        assert(errorTrueFromIndependent < 1)
        assert(errorIndependentFromPhase < 1)

        val endRotation = destinationPort.globalRotation() + shipPort.relativeRotation()
        val dockingPosition = endPortGlobalPos + (shipPort.relativePosition() * -1.0).rotated(endRotation)
        val dockingPositionError = (dockingPosition - phaseEndState.position).length
        val durationError = abs(phase.durationTicks - phase.ticksSinceStart)

        assert(iterations == expectedIterations)
        assert(finalT > 0.99)
        assert(targetError < 15)
        assert(trueError < 15)
        assert(dockingPositionError < 1)
        assert(durationError < 1)
    }

    @Test
    fun testDockingPortDrift(){
        DHServer.timer.cancel()
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val destinationStation = OrbiterManager.getStations().asSequence().first()
        val destinationPort = destinationStation.dockingPorts[0]
        val ship = Ship(
            ShipClassManager.getShipClass(DHServer.playerStartingShip)!!,
            ShipColor(Color.WHITE),
            ShipColor(Color.WHITE),
            startState,
            AIShipController()
        )
        var targetPortPositionFromPhase = Vector2.ZERO
        val shipPort = ship.myDockingPorts[0]
        val phase = NavigationRoute.trainPhase(0){ endTickEst ->
            val endVel = destinationPort.velocityAtTick(endTickEst)
            val endPortGlobalPos = destinationPort.globalPosAtTick(endTickEst)
            targetPortPositionFromPhase = endPortGlobalPos
            val endRotation = destinationPort.globalRotationAtTick(endTickEst) + shipPort.relativeRotation() //why do I keep having to offset by pi here
            val targetPos = endPortGlobalPos + (shipPort.relativePosition() * -1.0).rotated(endRotation)
            val endState = ShipState(targetPos, endRotation, endVel)
            //lastEstEndTime = endTimeEst
            BezierNavigationPhase(ship.type.mainThrust, ship.currentState, endState)
        }

        val duration = phase.durationTicks
        val targetPortPosIndependent = destinationPort.globalPosAtTick(duration)

        while(phase.hasNextStep()){
            phase.step()
            OrbiterManager.process(DHServer.TICK_LENGTH_SECONDS)
        }

        val targetPortTruePosition = destinationPort.globalPosition()

        val errorTrueFromPhase = (targetPortTruePosition - targetPortPositionFromPhase).length
        val errorTrueFromIndependent = (targetPortTruePosition - targetPortPosIndependent).length
        val errorIndependentFromPhase = (targetPortPosIndependent - targetPortPositionFromPhase).length
        assert(errorTrueFromPhase < 1)
        assert(errorTrueFromIndependent < 1)
        assert(errorIndependentFromPhase < 1)
    }

    @Test
    fun testDockingPortDrift2(){
        DHServer.timer.cancel()
        val startState = ShipState(Vector2.ZERO, 0.0, Vector2(10, 0))
        val destinationStation = OrbiterManager.getStations().asSequence().first()
        val destinationPort = destinationStation.dockingPorts[0]
        val ship = Ship(
            ShipClassManager.getShipClass(DHServer.playerStartingShip)!!,
            ShipColor(Color.WHITE),
            ShipColor(Color.WHITE),
            startState,
            AIShipController()
        )
        var targetPortPositionFromPhase = Vector2.ZERO
        val shipPort = ship.myDockingPorts[0]
        val phase = NavigationRoute.trainPhase(0){ endTickEst ->
            val endVel = destinationPort.velocityAtTick(endTickEst)
            val endPortGlobalPos = destinationPort.globalPosAtTick(endTickEst)
            targetPortPositionFromPhase = endPortGlobalPos
            val endRotation = destinationPort.globalRotationAtTick(endTickEst) + shipPort.relativeRotation() //why do I keep having to offset by pi here
            val targetPos = endPortGlobalPos + (shipPort.relativePosition() * -1.0).rotated(endRotation)
            val endState = ShipState(targetPos, endRotation, endVel)
            //lastEstEndTime = endTimeEst
            BezierNavigationPhase(ship.type.mainThrust, ship.currentState, endState)
        }

        val duration = phase.durationTicks
        val targetPortPosIndependent = destinationPort.globalPosAtTick(duration)

        var output: ShipState = startState
        while(phase.hasNextStep()){
            output = phase.step()
            OrbiterManager.process(DHServer.TICK_LENGTH_SECONDS)
        }

        val targetPortTruePosition = destinationPort.globalPosition()

        val errorTrueFromPhase = (targetPortTruePosition - targetPortPositionFromPhase).length
        val errorTrueFromIndependent = (targetPortTruePosition - targetPortPosIndependent).length
        val errorIndependentFromPhase = (targetPortPosIndependent - targetPortPositionFromPhase).length
        assert(errorTrueFromPhase < 1)
        assert(errorTrueFromIndependent < 1)
        assert(errorIndependentFromPhase < 1)
    }
}