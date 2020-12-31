package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.database.script.ScriptReader
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.wallet.Wallet
import com.dibujaron.distanthorizon.ship.*
import com.dibujaron.distanthorizon.utils.TimeUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

class Station(parentName: String?, stationName: String, properties: Properties) :
    Orbiter(parentName, stationName, properties) {

    val dockingPorts = LinkedList<StationDockingPort>()
    val displayName = properties.getProperty("displayName").trim()
    val splashTextList = ArrayList<String>()
    val dealerships = HashMap<Manufacturer, Int>()
    val navigable = properties.getProperty("navigable", "true").toBoolean()
    private val aiScripts: Map<Int, ScriptReader> = DHServer.getDatabase().getScriptDatabase()
        .selectScriptsForStation(this).asSequence()
        .filter { it.getSourceStation().navigable && it.getDestinationStation().navigable }
        .map { Pair(it.getDepartureTick(), it) }
        .toMap()

    private val commodityStores: Map<CommodityType, CommodityStore> = CommodityType
        .values()
        .asSequence()
        .map { CommodityStore(it, properties) }
        .map { Pair(it.type, it) }
        .toMap()

    init {
        dockingPorts.add(StationDockingPort(this, Vector2(7.0, 0.5), -90.0))
        dockingPorts.add(StationDockingPort(this, Vector2(-7.0, 0.5), 90.0))
        if (aiScripts.isNotEmpty()) {
            println("loaded ${aiScripts.size} ai scripts for station $name")
        }
        var index = 0
        var currentSplash = properties.getProperty("splash.$index", null)
        while (currentSplash != null) {
            splashTextList.add(currentSplash)
            index++
            currentSplash = properties.getProperty("splash.$index", null)
        }
        Manufacturer.values().forEach {
            val dealershipPercentage = properties.getProperty("dealership.${it.identifyingName}", "0").toInt()
            if (dealershipPercentage > 0) {
                dealerships[it] = dealershipPercentage
            }
        }
    }

    override fun tick() {
        commodityStores.values.forEach { it.tick() }
        val script = aiScripts[TimeUtils.getCurrentTickInCycle()]
        if (script != null) {
            //todo don't spawn if too many AI ships already
            println("initializing AI ship from station $name")
            ShipManager.addShip(AIShip(script.copy()))
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

    fun createShopMessage(player: Player): JSONObject {
        val changesEveryFiveMinutes = System.currentTimeMillis() / 300000
        val rand = Random(changesEveryFiveMinutes + displayName.hashCode())
        val retval = JSONObject()
        retval.put("identifying_name", name)
        retval.put("display_name", displayName)
        retval.put("description", splashTextList.random(rand))
        val commodities = JSONArray()
        commodityStores.values.asSequence()
            .map { it.createStoreJson() }
            .forEach { commodities.put(it) }
        retval.put("commodity_stores", commodities)
        val dealershipJson = JSONArray()
        Manufacturer.values().forEach {
            val percent = dealerships[it]
            if (percent != null) {
                val json = it.toJSON(player, rand, percent)
                if (!json.getJSONArray("ship_classes").isEmpty) {
                    dealershipJson.put(json)
                }
            }
        }
        retval.put("dealerships", dealershipJson)
        return retval
    }

    fun sellResourceToShip(resource: CommodityType, buyingWallet: Wallet, ship: Ship, quantity: Int) {
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

        if (buyingWallet.getBalance() < price) {
            val affordableQuantity = buyingWallet.getBalance() / store.price
            purchaseQuantity = affordableQuantity
        }
        val purchasePrice = store.price * purchaseQuantity
        buyingWallet.setBalance(buyingWallet.getBalance() - purchasePrice)
        store.quantityAvailable -= purchaseQuantity
        ship.updateHoldQuantity(resource, purchaseQuantity)
        //val holdStore = ship.hold.getOrPut(store.identifyingName, { 0 })
        //ship.hold[store.identifyingName] = holdStore + purchaseQuantity
    }

    fun buyResourceFromShip(resource: CommodityType, buyingWallet: Wallet, ship: Ship, quantity: Int) {
        val store = commodityStores.getValue(resource)
        var purchaseQuantity = quantity

        //first check if there's enough in player's hold to sell
        val availableQuantity = ship.getHoldQuantity(resource)
        if (purchaseQuantity > availableQuantity) {
            purchaseQuantity = availableQuantity
        }

        //now do it
        val purchasePrice = store.price * purchaseQuantity
        buyingWallet.setBalance(buyingWallet.getBalance() + purchasePrice)
        store.quantityAvailable += purchaseQuantity
        ship.updateHoldQuantity(resource, -purchaseQuantity)
        //val holdStore = ship.hold.getOrPut(store.identifyingName, { 0 })
        //ship.hold[store.identifyingName] = holdStore - purchaseQuantity
    }

    override fun createOrbiterJson(): JSONObject {
        val retval = super.createOrbiterJson()
        retval.put("display_name", displayName)
        retval.put("navigable", navigable)
        return retval
    }
}