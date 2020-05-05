package com.dibujaron.distanthorizon.orbiter

import com.dibujaron.distanthorizon.commodity.CommodityStore
import com.dibujaron.distanthorizon.commodity.CommodityType
import com.dibujaron.distanthorizon.docking.StationDockingPort
import org.json.JSONObject
import java.util.*

class Station(properties: Properties) : Orbiter(properties) {

    val dockingPorts = LinkedList<StationDockingPort>()
    private val commodityStores: Map<String, CommodityStore> = CommodityType
        .values()
        .asSequence()
        .map { it.identifyingName }
        .map { CommodityStore(it, properties) }
        .map { Pair(it.identifyingName, it)}
        .toMap()

    override fun toJSON(): JSONObject {
        val retval = super.toJSON()
        return retval
    }

    /*fun sellResource(resource: String, player: Player, quantity: Int)
    {
        val store = commodityStores.getValue(resource);
        player.balance -= store.buyPrice * quantity;
        store.quantityAvailable -= quantity
    }

    fun buyResource(resource: String, player: Player, quantity: Int)
    {
        val store = commodityStores.getValue(resource);
        player.balance += store.sellPrice * quantity;
        store.quantityAvailable -= quantity
    }*/
}