package com.dibujaron.distanthorizon.player

open class PlayerCompanionAI(val player: Player) {

    private val greetingsNamed = arrayListOf(
        "Hi %n, it's good to see you!",
        "Good morning, welcome back %n!",
        "Hello %n! Where are we heading today?",
        "Tip: press 'x' to toggle the breadcrumbs view.",
        "Tip: use /top to see the top players across all servers."
    )

    private val greetingsGeneric = arrayListOf(
        "Hi, it's good to see you!",
        "Good morning, welcome back!",
        "Hello! Where are we heading today?",
        "Tip: press 'x' to toggle the breadcrumbs view."
    )

    open fun getName(): String {
        return "KANE"
    }

    open fun getInitializationMessage(): String {
        return "--- Kotlin Automatic Noob Enlightener 4.47.2 initialized ---"
    }

    open fun getLoggedInGreeting(): String {
        return format(greetingsNamed.random())
    }

    open fun getGuestGreeting(): String {
        return format(greetingsGeneric.random())
    }

    open fun format(message: String): String {
        return message.replace("%n", player.getDisplayName())
    }

}