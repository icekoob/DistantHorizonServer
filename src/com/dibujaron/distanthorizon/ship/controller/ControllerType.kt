package com.dibujaron.distanthorizon.ship.controller

enum class ControllerType(val key: String) {
    ARTIFICIAL("ai"),
    PLAYER("player");

    fun toJSON(): String {
        return key
    }
}