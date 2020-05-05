package com.dibujaron.distanthorizon.orbiter

import java.io.File
import java.io.FileReader
import java.util.*
import kotlin.collections.HashMap

object OrbiterManager {
    private val orbitersMap: HashMap<String, Orbiter> = HashMap()
    private val planetsMap: HashMap<String, Planet> = HashMap()
    private val stationsMap: HashMap<String, Station> = HashMap()

    fun process(deltaSeconds: Double)
    {
        getOrbiters().forEach { it.process(deltaSeconds) }
    }
    fun getPlanet(name: String): Planet? {
        return planetsMap[name];
    }

    fun getPlanets(): Collection<Planet>{
        return planetsMap.values
    }

    fun getOrbiter(name: String): Orbiter? {
        return orbitersMap[name];
    }

    fun getOrbiters(): Collection<Orbiter>
    {
        return orbitersMap.values
    }

    fun getStation(name: String): Station? {
        return stationsMap[name];
    }

    fun getStations(): Collection<Station>
    {
        return stationsMap.values
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

        orbitersMap.values.forEach{it.initialize()}
    }

    fun loadProperties(reader: FileReader): Properties {
        val props = Properties()
        props.load(reader)
        return props
    }
}