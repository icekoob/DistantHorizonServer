package com.dibujaron.distanthorizon.ship

import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class Manufacturer(
    val identifyingName: String,
    val displayNameShort: String,
    val displayNameLong: String = displayNameShort,
    val splashes: List<String>
) {
    ALDRIN(
        "aldrin", "Aldrin", "Aldrin Aeronautics", listOf(
            "Aldrin Aeronautics -- The galaxy's finest vessels for the discerning buyer.",
            "'Our designs are made for the rich and powerful. Are you sure *you* should be shopping here?'",
            "'Aldrin Aeronautics is proud to introduce the KX-6, the galaxy's most revolutionary exploration craft!'",
            "'Is price the only thing holding you back?'"
        )
    ),
    RIJAY(
        "rijay", "Rijay", "Rijay Drive Yards", listOf(
            "Rijay Drive Yards -- Live Your Dream!",
            "Rijay ships aren't always reliable, but they're certainly fast.",
            "'Aerodynamics stopped mattering once we got to space, but curves never stopped looking good!'",
            "Rijay started out as a manufacturer of racing ships, but they now offer a complete line of vessels.",
            "'Buy now -- this price won't last long!'",
            "'Can I interest you in a rust-prevention coat?'",
            "Rijay ships are favored by Skywatch for their speed. You hope nobody will think you're a cop."
        )
    ),
    PHE(
        "phe", "P.H.E.", "Porter Heavy Engineering", listOf(
            "Porter Heavy Engineering -- Built To Last!",
            "'She don't look like much, but she's got it where it counts!'",
            "'You buy this ship, treat her right, she'll be with you for the rest of your life.'",
            "Porter ships aren't pretty, but they sure are sturdy.",
            "Porter designs are mostly industrial, but they offer a few general-commerce ships as well.",
            "'Fuel efficiency?... Well...'",
            "'What? No, it's never been crashed. It's supposed to look like that.'"
        )
    );

    companion object {
        fun fromString(identifyingName: String): Manufacturer {
            val result = values().asSequence()
                .find { it.identifyingName == identifyingName }
            return result!!
        }
    }

    fun toJSON(random: Random, percentage: Int): JSONObject {
        val retval = JSONObject()
        retval.put("identifying_name", identifyingName)
        retval.put("display_name_short", displayNameShort)
        retval.put("display_name_long", displayNameLong)
        retval.put("description", splashes.random(random))
        val shipClasses = JSONArray()
        ShipClassManager.getShipClasses().asSequence()
            .filter { it.manufacturer == this }
            .map { it.toJSON(random, percentage) }
            .filter { !it.getJSONArray("colors").isEmpty }
            .forEach { shipClasses.put(it) }
        retval.put("ship_classes", shipClasses)
        return retval
    }
}