package com.dibujaron.distanthorizon.pathfinder

class PathInputs(var enginesActive: Boolean, var rotatingLeft: Boolean, var rotatingRight: Boolean) {
    companion object Factory {
        val allInputCombinations = arrayOf(
            PathInputs(enginesActive = false, rotatingLeft = false, rotatingRight = false),
            PathInputs(enginesActive = false, rotatingLeft = false, rotatingRight = true),
            PathInputs(enginesActive = false, rotatingLeft = true, rotatingRight = false),
            PathInputs(enginesActive = true, rotatingLeft = false, rotatingRight = false),
            PathInputs(enginesActive = true, rotatingLeft = false, rotatingRight = true),
            PathInputs(enginesActive = true, rotatingLeft = true, rotatingRight = false))
    }
}
