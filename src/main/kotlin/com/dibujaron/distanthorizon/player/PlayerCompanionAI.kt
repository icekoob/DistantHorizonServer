package com.dibujaron.distanthorizon.player

import java.util.*

open class PlayerCompanionAI(val playerID: UUID) {

    private val greetings = arrayListOf(
        "Hi, it's good to see you!",
        "Good morning, welcome back!",
        "Hello! Where are we heading today?",
    )

    open fun getName(): String {
        return "KANE"
    }

    open fun getInitializationMessage(): String {
        return "--- Kotlin Automatic Noob Enlightener 4.47.2 initialized ---"
    }

    open fun getGreeting(): String {
        return greetings.random()
    }

    open fun getFirstTimeGreeting(): String {
        return getGreeting()
    }

}