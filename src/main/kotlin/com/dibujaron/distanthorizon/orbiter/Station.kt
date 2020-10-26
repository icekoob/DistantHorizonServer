package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.player.Account
import com.dibujaron.distanthorizon.script.ScriptReader
import com.dibujaron.distanthorizon.ship.AIShip
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipManager
import com.dibujaron.distanthorizon.ship.ShipState
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.floor

class Station(parentName: String?, stationName: String, properties: Properties) : Orbiter(parentName, stationName, properties) {

    val dockingPorts = LinkedList<StationDockingPort>()
    val displayName = properties.getProperty("displayName").trim()
    val splashTextList = ArrayList<String>()
    private val aiScripts: Map<Int, ScriptReader> = DHServer.getScriptDatabase()
        .selectScriptsForStation(this).asSequence()
        .map { Pair(it.getDepartureTick(), it) }
        .toMap()

    private val commodityStores: Map<String, CommodityStore> = CommodityType
        .values()
        .asSequence()
        .map { CommodityStore(it, properties) }
        .map { Pair(it.identifyingName, it) }
        .toMap()

    init {
        dockingPorts.add(StationDockingPort(this, Vector2(7.0, 0.5), -90.0))
        dockingPorts.add(StationDockingPort(this, Vector2(-7.0, 0.5), 90.0))
        if (aiScripts.isNotEmpty()) {
            println("loaded ${aiScripts.size} ai scripts for station $name")
        }
        var index = 0
        var currentSplash = properties.getProperty("splash.$index", null)
        while(currentSplash != null){
            splashTextList.add(currentSplash)
            index++
            currentSplash = properties.getProperty("splash.$index", null)
        }
    }

    override fun tick() {
        commodityStores.values.forEach { it.tick() }
        val script = aiScripts[DHServer.getCurrentTickInCycle()]
        if (script != null) {
            println("initializing AI ship from station $name")
            ShipManager.markForAdd(AIShip(script.copy()))
        }
        super.tick()
    }

    fun getState(): ShipState {
        return ShipState(globalPos(), globalRotation(), velocity())
    }

    fun globalRotation(): Double {
        val vecToParent = relativePos * -1.0;
        return vecToParent.angle;
    }

    fun globalRotationAtTime(timeOffset: Double): Double {
        val vecToParentAtTime = relativePosAtTime(timeOffset) * -1.0
        return vecToParentAtTime.angle
    }

    fun globalRotationAtTick(tickOffset: Double): Double {
        val vecToParentAtTime = relativePosAtTick(tickOffset) * -1.0
        return vecToParentAtTime.angle
    }

    fun createdShopMessage(): JSONObject {
        val retval = JSONObject()
        retval.put("identifying_name", name)
        retval.put("display_name", displayName)
        retval.put("description", splashTextList.random())
        val arr = JSONArray()
        commodityStores.values.asSequence()
            .map { it.createStoreJson() }
            .forEach { arr.put(it) }
        retval.put("commodity_stores", arr)
        return retval
    }

    fun sellResourceToShip(resource: String, buyingAccount: Account, ship: Ship, quantity: Int) {
        val store = commodityStores.getValue(resource)
        val price = store.price * quantity
        var purchaseQuantity = quantity

        //first check if there's enough on the station
        val availableQuantity = store.quantityAvailable
        if (purchaseQuantity > availableQuantity) {
            purchaseQuantity = availableQuantity
        }

        //now check if there's room in the hold
        val spaceInHold = ship.holdCapacity - ship.holdOccupied()
        if (purchaseQuantity > spaceInHold) {
            purchaseQuantity = spaceInHold
        }

        if (buyingAccount.balance < price) {
            val affordableQuantity = floor(buyingAccount.balance / store.price).toInt()
            purchaseQuantity = affordableQuantity
        }
        val purchasePrice = store.price * purchaseQuantity
        buyingAccount.balance = buyingAccount.balance - purchasePrice
        store.quantityAvailable -= purchaseQuantity
        val holdStore = ship.hold.getOrPut(store.identifyingName, { 0 })
        ship.hold[store.identifyingName] = holdStore + purchaseQuantity
    }

    fun buyResourceFromShip(resource: String, buyingAccount: Account, ship: Ship, quantity: Int) {
        val store = commodityStores.getValue(resource)
        var purchaseQuantity = quantity

        //first check if there's enough in player's hold to sell
        val availableQuantity = ship.hold[resource] ?: 0
        if (purchaseQuantity > availableQuantity) {
            purchaseQuantity = availableQuantity
        }

        //now do it
        val purchasePrice = store.price * purchaseQuantity
        buyingAccount.balance = buyingAccount.balance + purchasePrice
        store.quantityAvailable += purchaseQuantity
        val holdStore = ship.hold.getOrPut(store.identifyingName, { 0 })
        ship.hold[store.identifyingName] = holdStore - purchaseQuantity
    }

    override fun createOrbiterJson(): JSONObject {
        val retval = super.createOrbiterJson()
        retval.put("display_name", displayName)
        return retval
    }
}