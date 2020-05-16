package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.docking.StationDockingPort
import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.ship.Ship
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.math.floor

class Station(properties: Properties) : Orbiter(properties) {

    val dockingPorts = LinkedList<StationDockingPort>()
    val displayName = properties.getProperty("displayName").trim()
    val description = properties.getProperty("description").trim()
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

    override fun createOrbiterJson(): JSONObject {
        val retval = super.createOrbiterJson()
        return retval
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

    fun sellResourceToPlayer(resource: String, player: Player, ship: Ship, quantity: Int) {
        val store = commodityStores.getValue(resource)
        val price = store.buyPrice * quantity
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

        if (player.balance < price){
            val affordableQuantity = floor(player.balance / store.buyPrice).toInt()
            purchaseQuantity = affordableQuantity
        }
        val purchasePrice = store.buyPrice * purchaseQuantity
        player.balance = player.balance - purchasePrice
        store.quantityAvailable -= purchaseQuantity
        val holdStore = ship.hold.getOrPut(store.identifyingName, {0})
        ship.hold[store.identifyingName] = holdStore + purchaseQuantity
        println("player ${player.uuid} bought $purchaseQuantity of ${store.identifyingName} for $purchasePrice")
    }

    fun buyResourceFromPlayer(resource: String, player: Player, ship: Ship, quantity: Int) {
        val store = commodityStores.getValue(resource)
        var purchaseQuantity = quantity

        //first check if there's enough in player's hold to sell
        val availableQuantity = ship.hold[resource]?:0
        if (purchaseQuantity > availableQuantity) {
            purchaseQuantity = availableQuantity
        }

        //now do it
        val purchasePrice = store.sellPrice * purchaseQuantity
        player.balance = player.balance + purchasePrice
        store.quantityAvailable += purchaseQuantity
        val holdStore = ship.hold.getOrPut(store.identifyingName, {0})
        ship.hold[store.identifyingName] = holdStore - purchaseQuantity
        println("player ${player.uuid} sold $purchaseQuantity of ${store.identifyingName} for $purchasePrice")
    }
}