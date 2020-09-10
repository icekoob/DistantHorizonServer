package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.Vector2
import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.pow

object OrbiterManager {
    private const val MIN_GRAVITY_FORCE_CUTOFF = 0.01

    private val orbitersMap: HashMap<String, Orbiter> = HashMap()
    private val planetsMap: HashMap<String, Planet> = HashMap()
    private val stationsMap: HashMap<String, Station> = HashMap()
    private const val gravityConstantFudge = 50.0
    val gravityConstant = 6.67408 * 10.0.pow(-11.0) * gravityConstantFudge

    fun tick() {
        getOrbiters().forEach { it.tick() }
    }

    fun getPlanet(name: String): Planet? {
        return planetsMap[name];
    }

    fun getPlanets(): Collection<Planet> {
        return planetsMap.values
    }

    fun getOrbiter(name: String): Orbiter? {
        return orbitersMap[name];
    }

    fun getOrbiters(): Collection<Orbiter> {
        return orbitersMap.values
    }

    fun getStation(name: String): Station? {
        return stationsMap[name];
    }

    fun getStations(): Collection<Station> {
        return stationsMap.values
    }

    fun calculateGravityAtTick(tickOffset: Double, globalPosAtTick: Vector2): Vector2 {
        var accel = Vector2(0, 0)
        getPlanets().asSequence()
            .forEach {
                val planetPosAtTick = it.globalPosAtTick(tickOffset)
                val offset = (planetPosAtTick - globalPosAtTick)
                var rSquared = offset.lengthSquared
                if (rSquared < it.minRadiusSquared) {
                    rSquared = it.minRadiusSquared.toDouble()
                }
                val forceMag = gravityConstant * it.mass / rSquared
                if(forceMag > MIN_GRAVITY_FORCE_CUTOFF) {
                    accel += (offset.normalized() * forceMag)
                }
            }
        return accel
    }

    init {
        File("./world/planets").walk()
            .filter { it.name.endsWith(".properties") }
            .map { FileReader(it) }
            .map { loadProperties(it) }
            .map { Planet(it) }
            .forEach {
                orbitersMap[it.name] = it
                planetsMap[it.name] = it
            }

        File("./world/stations").walk()
            .filter { it.name.endsWith(".properties") }
            .map { FileReader(it) }
            .map { loadProperties(it) }
            .map { Station(it) }
            .forEach {
                orbitersMap[it.name] = it
                stationsMap[it.name] = it
            }

        orbitersMap.values.forEach { it.initialize() }
    }

    fun loadProperties(reader: FileReader): Properties {
        val props = Properties()
        props.load(reader)
        return props
    }
}