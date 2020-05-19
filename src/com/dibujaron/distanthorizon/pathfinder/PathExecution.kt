package com.dibujaron.distanthorizon.pathfinder

import com.dibujaron.distanthorizon.orbiter.Station

class PathExecution(
    val start: Station,
    val end: Station,
    val timeStepLength: Double,
    val mainEngineThrust: Double,
    val rotationPower: Double,
    val maxAcceleration: Double
) {}