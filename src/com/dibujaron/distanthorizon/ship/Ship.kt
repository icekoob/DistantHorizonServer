package com.dibujaron.distanthorizon.ship

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.docking.ShipDockingPort
import com.dibujaron.distanthorizon.orbiter.OrbiterManager
import org.json.JSONObject
import java.lang.Math.pow
import java.util.*
import kotlin.math.pow

class Ship(
    val type: ShipModel,
    var velocity: Vector2,
    var globalPos: Vector2,
    var rotation: Double
) {
    val gravityConstant = 6.67408 * 10.0.pow(-11.0)
    val uuid = UUID.randomUUID()
    //controls
    var mainEnginesActive: Boolean = false
    var portThrustersActive: Boolean = false
    var stbdThrustersActive: Boolean = false
    var foreThrustersActive: Boolean = false
    var aftThrustersActive: Boolean = false
    var rotatingLeft: Boolean = false
    var rotatingRight: Boolean = false
    var docked: Boolean = false

    val myDockingPorts = LinkedList<ShipDockingPort>()
    fun process(delta: Double) {
        if (!docked) {
            if (mainEnginesActive) {
                velocity += Vector2(0, -type.mainThrust).rotated(rotation) * delta
            }
            if (stbdThrustersActive) {
                velocity += Vector2(-type.manuThrust, 0).rotated(rotation) * delta
            }
            if (portThrustersActive) {
                velocity += Vector2(type.manuThrust, 0).rotated(rotation) * delta
            }
            if (foreThrustersActive) {
                velocity += Vector2(0, type.manuThrust).rotated(rotation) * delta
            }
            if (aftThrustersActive) {
                velocity += Vector2(0, -type.manuThrust).rotated(rotation) * delta
            }
            if (rotatingLeft) {
                rotation -= type.rotationPower * delta
            }
            if (rotatingRight) {
                rotation += type.rotationPower * delta
            }
            velocity += gravityAccelAtTime(0.0, globalPos)
            globalPos += velocity * delta
            //velocity += gravityAccelAtTime(delta, globalPos)
        }
    }

    fun toJSON(): JSONObject
    {
        val retval = JSONObject()
        retval.put("id", uuid)
        retval.put("type", type.qualifiedName())
        retval.put("velocity", velocity.toJSON())
        retval.put("global_pos", globalPos.toJSON())
        retval.put("rotation", rotation)
        retval.put("main_engines", mainEnginesActive)
        retval.put("port_thrusters", portThrustersActive)
        retval.put("stbd_thrusters", stbdThrustersActive)
        retval.put("fore_thrusters", foreThrustersActive)
        retval.put("aft_thrusters", aftThrustersActive)
        retval.put("rotating_left", rotatingLeft)
        retval.put("rotating_right", rotatingRight)
        return retval
    }

    fun gravityAccelAtTime(timeOffset: Double, globalPosAtTime: Vector2): Vector2 {
        var accel = Vector2(0, 0)
        OrbiterManager.getPlanets().asSequence()
            .map {
                val planetPosAtTime = it.globalPosAtTime(timeOffset)
                val offset = (planetPosAtTime - globalPosAtTime)
                var rSquared = offset.lengthSquared
                if (rSquared < it.minRadiusSquared) {
                    rSquared = it.minRadiusSquared.toDouble()
                }
                val forceMag = gravityConstant * it.mass / rSquared
                offset.normalized() * forceMag
            }
            .forEach { accel += it }
        return accel
    }

    fun attemptDock(){
        val maxDockDist = 50.0
        val maxDistSquared = pow(2.0, maxDockDist)

        val maxClosingSpeed = 500.0
        val maxClosingSpeedSquared = pow(2.0, maxClosingSpeed)

        val match = OrbiterManager.getStations().asSequence()
            .flatMap{it.dockingPorts.asSequence()}
            .flatMap{stationPort -> myDockingPorts.asSequence()
                .map{shipPort -> Pair(shipPort, stationPort)}}
            .filter{(it.first.getVelocity() - it.second.getVelocity()).lengthSquared < maxClosingSpeedSquared}
            .map{Triple(it.first, it.second, (it.first.globalPosition() - it.second.globalPosition()).lengthSquared)}
            .filter{it.third < maxDistSquared}
            .minBy{it.third}

        if(match != null){
            val bestShipPort = match.first
            val bestStationPort = match.second
            println("best dock is ship port $bestShipPort to $bestStationPort")
        }
    }
}