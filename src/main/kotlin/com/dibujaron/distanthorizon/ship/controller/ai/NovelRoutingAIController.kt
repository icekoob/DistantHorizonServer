package com.dibujaron.distanthorizon.ship.controller.ai

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.ShipState
import com.dibujaron.distanthorizon.ship.controller.ControllerType
import com.dibujaron.distanthorizon.ship.controller.ShipController
import com.dibujaron.distanthorizon.ship.controller.ai.nav.Navigation
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.roundToInt

const val STATES_TO_PREDICT = DHServer.SHIP_HEARTBEATS_EVERY * 2

//AIs based on this actually composes a new original route. They pass a script to the client that isn't based on inputs, rather exact positions.
abstract class NovelRoutingAIController : ShipController() {

    abstract fun createNavigation(startState: ShipState, targetState: ShipState, targetStation: Station, targetStationPort: StationDockingPort, myPort: ShipDockingPort): Navigation

    var currentNav: Navigation? = null
    var currentStep = 0
    var nextDepartureTick = computeNextDepartureTick()

    override fun isRequestingUndock(): Boolean {
        return if (DHServer.getCurrentTick() > nextDepartureTick) {
            currentNav = plotNewCourse()
            true;
        } else {
            false;
        }
    }

    open fun plotNewCourse() : Navigation {
        val stations = OrbiterManager.getStations().asSequence().filter {
            ship.dockedToPort?.station != it
        }.filter{it.getStar().name.contains("Regalis")}.toList()
        val destStation = stations.random()
        val myPort = ship.myDockingPorts.random()
        val destPort = destStation.dockingPorts.random()
        return trainPhase(myPort, destPort) { endState -> createNavigation(ship.currentState, endState, destStation, destPort, myPort) }
    }

    override fun getType(): ControllerType {
        return ControllerType.ARTIFICIAL
    }

    override fun getHeartbeat(): JSONObject {
        val retval = JSONObject()
        val nav = currentNav
        if (nav != null) {
            val steps = JSONArray()
            nav.getSteps(currentStep, STATES_TO_PREDICT).map { it.toJSON() }.forEach { steps.put(it) }
            retval.put("current_step", currentStep)
            retval.put("final_step", nav.numSteps())
            retval.put("future_steps", steps)
            //retval.put("target_station", nav.targetStation.name)
            //retval.put("target_port", nav.targetStationPort.)
        }
        return retval
    }

    override fun computeNextState(): ShipState {
        val nav = currentNav
        return if (nav != null) {
            currentStep++
            if (currentStep < nav.numSteps()) {
                nav.getSteps(currentStep, 1).first()
            } else {
                currentNav = null
                dock()
                ship.currentState
            }
        } else {
            //this happens if it's brand new.
            dock()
            ship.currentState
        }
    }

    open fun dock(){
        currentNav = null
        ship.attemptDock(2000.0, 2000.0)
        if (ship.isDocked()) {
            println("AI Ship ${ship.uuid} docked at ${ship.dockedToPort?.station}")
            nextDepartureTick = computeNextDepartureTick()
        } else {
            throw java.lang.IllegalStateException("AI ship ${ship.uuid} should have docked but failed to dock.")
        }
    }

    open fun getMinimumStationDwellTicks(): Int {
        return 5 * 60
    }

    open fun getMaximumStationDwellTicks(): Int {
        return 20 * 60
    }

    private fun computeNextDepartureTick(): Int {
        val minDwell = getMinimumStationDwellTicks()
        val dwellRange = getMaximumStationDwellTicks() - minDwell
        return DHServer.getCurrentTick() + minDwell + (Math.random() * dwellRange).roundToInt()
    }

    companion object {
        fun trainPhase(
            shipPort: ShipDockingPort,
            destination: StationDockingPort,
            endStateToNavFunc: (endState: ShipState) -> Navigation
        ): Navigation {
            return trainPhase(0, shipPort, destination, endStateToNavFunc)
        }

        private fun trainPhase(
            startTickOffset: Int,
            shipPort: ShipDockingPort,
            destination: StationDockingPort,
            endStateToNavFunc: (endState: ShipState) -> Navigation
        ): Navigation {
            var iterations = 0
            val previousEstimations = LinkedList<Int>()
            previousEstimations.addLast(startTickOffset)
            while (iterations < 100000) {
                if (iterations > 6) {
                    previousEstimations.pollFirst()
                }
                val previousEstimate = previousEstimations.last
                val currentGuessEndState =
                    computeDesiredEndStateFromEndTick(previousEstimate.toDouble(), shipPort, destination)
                val currentGuessNav = endStateToNavFunc(currentGuessEndState)
                val estimatedEndTick = startTickOffset + currentGuessNav.numSteps()
                val movingAverage = previousEstimations.asSequence().sum() / previousEstimations.size
                val diff = estimatedEndTick - movingAverage
                if (diff < 0.01) {
                    return currentGuessNav
                }
                previousEstimations.addLast(estimatedEndTick)
                iterations++
            }
            throw IllegalStateException("Phase training failed to converge.")
        }

        private fun computeDesiredEndStateFromEndTick(
            endTick: Double,
            shipPort: ShipDockingPort,
            destination: StationDockingPort
        ): ShipState {
            val endVel = destination.velocityAtTick(endTick)
            val endPortGlobalPos = destination.globalPosAtTick(endTick)
            val myPortRelative = shipPort.relativePosition()
            val endRotation =
                destination.globalRotationAtTick(endTick) + shipPort.relativeRotation() //why do I keep having to offset by pi here
            val targetPos = endPortGlobalPos + (myPortRelative * -1.0).rotated(endRotation)
            return ShipState(targetPos, endRotation, endVel)
        }
    }
}