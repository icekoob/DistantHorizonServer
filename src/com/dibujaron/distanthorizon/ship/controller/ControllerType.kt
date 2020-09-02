package com.dibujaron.distanthorizon.ship.controller

import org.json.JSONObject

enum class ControllerType(val key: String) {
    ARTIFICIAL("ai"),
    PLAYER("player");

    fun toJSON(): String
    {
        return key
    }
}