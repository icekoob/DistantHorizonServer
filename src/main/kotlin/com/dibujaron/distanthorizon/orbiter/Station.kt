package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.player.Account
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipState
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.floor

class Station(properties: Properties) : Orbiter(properties) {

    val dockingPorts = LinkedList<StationDockingPort>()
    val displayName = properties.getProperty("displayName").trim()
    val description = properties.getProperty("description").trim()
    public var ticks = 0
    private val commodityStores: Map<String, CommodityStore> = CommodityType
        .values()
        .asSequence()
        .map { CommodityStore(it, properties) }
        .map { Pair(it.identifyingName, it) }
        .toMap()

    init {
        dockingPorts.add(StationDockingPort(this, Vector2(7.0, 0.5), -90.0))
        dockingPorts.add(StationDockingPort(this, Vector2(-7.0, 0.5), 90.0))
    }

    override fun tick() {
        commodityStores.values.forEach{it.tick()}
        ticks++
    }

    fun getState(): ShipState
    {
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
        retval.put("description", description)
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

        if (buyingAccount.balance < price){
            val affordableQuantity = floor(buyingAccount.balance / store.price).toInt()
            purchaseQuantity = affordableQuantity
        }
        val purchasePrice = store.price * purchaseQuantity
        buyingAccount.balance = buyingAccount.balance - purchasePrice
        store.quantityAvailable -= purchaseQuantity
        val holdStore = ship.hold.getOrPut(store.identifyingName, {0})
        ship.hold[store.identifyingName] = holdStore + purchaseQuantity
    }

    fun buyResourceFromShip(resource: String, buyingAccount: Account, ship: Ship, quantity: Int) {
        val store = commodityStores.getValue(resource)
        var purchaseQuantity = quantity

        //first check if there's enough in player's hold to sell
        val availableQuantity = ship.hold[resource]?:0
        if (purchaseQuantity > availableQuantity) {
            purchaseQuantity = availableQuantity
        }

        //now do it
        val purchasePrice = store.price * purchaseQuantity
        buyingAccount.balance = buyingAccount.balance + purchasePrice
        store.quantityAvailable += purchaseQuantity
        val holdStore = ship.hold.getOrPut(store.identifyingName, {0})
        ship.hold[store.identifyingName] = holdStore - purchaseQuantity
    }
}