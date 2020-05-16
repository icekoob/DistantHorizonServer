package com.dibujaron.distanthorizon.player

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipManager
import com.dibujaron.distanthorizon.ship.ShipClassManager
import io.javalin.websocket.WsContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class Player(val connection: WsContext) {
    val uuid: UUID = UUID.randomUUID()
    val myShip: Ship =
        Ship(ShipClassManager.getShipClass("rijay.mockingbird")!!, Vector2(375, 3180), 0.0)
    var balance = 1000.0

    init {
        ShipManager.markForAdd(myShip)
    }

    fun onMessageFromClient(message: JSONObject) {
        //so far we only have one client message, and it's inputs.
        val messageType = message.getString("message_type")
        if (messageType == "ship_inputs") {
            myShip.receiveInputsAndBroadcast(message)
        } else if (messageType == "dock_or_undock") {
            myShip.dockOrUndock()
            sendTradeMenuMessage()
        } else if (messageType == "purchase_from_station") {
            val dockedTo = myShip.dockedToPort
            if (dockedTo != null) {
                val station = dockedTo.station
                val commodity = message.getString("commodity_name")
                val quantity = message.getInt("quantity")
                station.sellResourceToPlayer(commodity, this, myShip, quantity)
                sendTradeMenuMessage()
            }
        } else if (messageType == "sell_to_station") {
            val dockedTo = myShip.dockedToPort
            if (dockedTo != null) {
                val station = dockedTo.station
                val commodity = message.getString("commodity_name")
                val quantity = message.getInt("quantity")
                station.buyResourceFromPlayer(commodity, this, myShip, quantity)
                sendTradeMenuMessage()
            }
        }
    }

    fun sendTradeMenuMessage() {
        val dockedTo = myShip.dockedToPort
        if (dockedTo != null) {
            val dockedToStation = dockedTo.station
            val stationInfo = dockedToStation.createdShopMessage();
            val myMessage = createMessage("trade_menu_info")
            myMessage.put("station_info", stationInfo)
            myMessage.put("player_balance", balance)
            myMessage.put("hold_space", myShip.holdCapacity - myShip.holdOccupied())
            val holdInfo: JSONObject = myShip.createHoldStatusMessage()
            myMessage.put("hold_contents", holdInfo)
            sendMessage(myMessage)
        } else {
            val myMessage = createMessage("trade_menu_close")
            sendMessage(myMessage)
        }
    }

    fun sendWorldState(worldStateMessage: JSONObject) {
        val myMessage = createMessage("world_state")
        myMessage.put("world_state", worldStateMessage)
        sendMessage(myMessage)
    }

    fun sendShipDocked(shipDockedMessage: JSONObject) {
        val myMessage = createMessage("ship_docked")
        myMessage.put("ship_docked", shipDockedMessage);
        sendMessage(myMessage)
    }

    fun sendShipUndocked(shipUndockedMessage: JSONObject)
    {
        val myMessage = createMessage("ship_undocked")
        myMessage.put("ship_undocked", shipUndockedMessage);
        sendMessage(myMessage)
    }

    fun sendShipInputsUpdate(shipInputsUpdate: JSONObject) {
        val myMessage = createMessage("ship_inputs")
        myMessage.put("ship_inputs", shipInputsUpdate)
        sendMessage(myMessage)
    }

    fun sendShipHeartbeats(shipHeartbeats: JSONArray) {
        val myMessage = createMessage("ship_heartbeats")
        myMessage.put("ship_heartbeats", shipHeartbeats)
        sendMessage(myMessage)
    }

    fun sendInitialShipsState(ships: JSONArray) {
        val myMessage = createMessage("ships_initial_state")
        myMessage.put("ships_initial_state", ships)
        sendMessage(myMessage)
    }

    fun createMessage(type: String): JSONObject {
        val message = JSONObject();
        message.put("message_type", type)
        message.put("player_id", uuid.toString())
        message.put("ship_id", myShip.uuid.toString())
        return message
    }

    fun sendMessage(message: JSONObject) {
        val messageStr = message.toString()
        connection.send(messageStr)
    }

}