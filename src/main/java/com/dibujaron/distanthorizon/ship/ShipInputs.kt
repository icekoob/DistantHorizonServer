package com.dibujaron.distanthorizon.ship

import org.json.JSONObject

class ShipInputs(
    val mainEnginesActive: Boolean = false,
    val portThrustersActive: Boolean = false,
    val stbdThrustersActive: Boolean = false,
    val foreThrustersActive: Boolean = false,
    val aftThrustersActive: Boolean = false,
    val tillerLeft: Boolean = false,
    val tillerRight: Boolean = false
) {

    constructor(inputsJson: JSONObject) : this(
        mainEnginesActive = inputsJson.getBoolean("main_engines_pressed"),
        portThrustersActive = inputsJson.getBoolean("port_thrusters_pressed"),
        stbdThrustersActive = inputsJson.getBoolean("stbd_thrusters_pressed"),
        foreThrustersActive = inputsJson.getBoolean("fore_thrusters_pressed"),
        aftThrustersActive = inputsJson.getBoolean("aft_thrusters_pressed"),
        tillerLeft = inputsJson.getBoolean("rotate_left_pressed"),
        tillerRight = inputsJson.getBoolean("rotate_right_pressed")
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShipInputs

        if (mainEnginesActive != other.mainEnginesActive) return false
        if (portThrustersActive != other.portThrustersActive) return false
        if (stbdThrustersActive != other.stbdThrustersActive) return false
        if (foreThrustersActive != other.foreThrustersActive) return false
        if (aftThrustersActive != other.aftThrustersActive) return false
        if (tillerLeft != other.tillerLeft) return false
        if (tillerRight != other.tillerRight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mainEnginesActive.hashCode()
        result = 31 * result + portThrustersActive.hashCode()
        result = 31 * result + stbdThrustersActive.hashCode()
        result = 31 * result + foreThrustersActive.hashCode()
        result = 31 * result + aftThrustersActive.hashCode()
        result = 31 * result + tillerLeft.hashCode()
        result = 31 * result + tillerRight.hashCode()
        return result
    }
}