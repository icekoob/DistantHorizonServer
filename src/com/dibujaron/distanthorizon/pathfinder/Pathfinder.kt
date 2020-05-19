package com.dibujaron.distanthorizon.pathfinder

import com.dibujaron.distanthorizon.orbiter.Station
import com.dibujaron.distanthorizon.ship.Ship
import java.util.*

object Pathfinder {

    const val TIME_STEP = 1 / 60.0
    fun findPath(start: Station, end: Station, startTime: Double, ship: Ship, maxAccel: Double): LinkedList<PathState>?
    {
        val execution = PathExecution(start, end, TIME_STEP, ship.type.mainThrust, ship.type.rotationPower, maxAccel)
        val stateScoreComparator: Comparator<PathState> = compareBy { it.f }
        val openList = PriorityQueue(stateScoreComparator)
        val closedList = PriorityQueue(stateScoreComparator)
        val startingInputs = PathInputs(false, false, false)
        val startState =
            PathState(execution, null, 0, start.globalPos(), start.velocity(), start.globalRotation(), startingInputs)
        openList.add(startState)

        var winningState: PathState? = null
        while (!openList.isEmpty()) {
            val q = openList.poll()
            val endPosWhenQ = end.globalPosAtTime(q.time)
            if((endPosWhenQ - q.globalPos).lengthSquared < 100){
                winningState = q;
                break;
            }
            q.nextStates(TIME_STEP).filter { nextState ->
                openList.asSequence()
                    .filter { olItem -> olItem.roughlyEqualTo(nextState) }
                    .filter { olItem -> olItem.f < nextState.f }.any()
            }.filterNot { nextState ->
                closedList.asSequence()
                    .filter { clItem -> clItem.roughlyEqualTo(nextState) }
                    .filter { clItem -> clItem.f < nextState.f }.any()
            }.forEach { openList.add(it) }
            closedList.add(q)
        }

        val stateThatWon = winningState
        return if(stateThatWon == null) {
            null
        } else {
            val retval = LinkedList<PathState>()
            var s: PathState = stateThatWon
            retval.add(s)
            var prior = s.priorState
            while(prior != null){
                s = prior
                prior = s.priorState
                retval.add(s)
            }
            retval
        }
    }
}