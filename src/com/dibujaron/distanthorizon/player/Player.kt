package com.dibujaron.distanthorizon.player

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.*
import com.dibujaron.distanthorizon.ship.controller.PlayerShipController
import io.javalin.websocket.WsContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class Player(val connection: WsContext) {
    val uuid: UUID = UUID.randomUUID()
    val myShipController: PlayerShipController = PlayerShipController()
    val ship: Ship = Ship(
        ShipClassManager.getShipClass("rijay.mockingbird")!!,
        ShipColor.random(),
        ShipColor.random(),
        ShipState(Vector2(375, 3180), 0.0, Vector2.ZERO),
        myShipController
    )
    var account = Account()

    init {
        ShipManager.markForAdd(ship)
    }

    fun onMessageFromClient(message: JSONObject) {
        //so far we only have one client message, and it's inputs.
        val messageType = message.getString("message_type")
        if (messageType == "ship_inputs") {
            val inputs = ShipInputs(message)
            myShipController.receiveInputChange(inputs)
        } else if (messageType == "dock_or_undock") {
            myShipController.dockOrUndock()
            sendTradeMenuMessage()
        } else if (messageType == "purchase_from_station") {
            if (ship.isDocked()) {
                val commodity = message.getString("commodity_name")
                val quantity = message.getInt("quantity")
                ship.buyResourceFromStation(commodity, account, quantity)
                sendTradeMenuMessage()
            }
        } else if (messageType == "sell_to_station") {
            if (ship.isDocked()) {
                val commodity = message.getString("commodity_name")
                val quantity = message.getInt("quantity")
                ship.sellResourceToStation(commodity, account, quantity)
                sendTradeMenuMessage()
            }
        }
    }

    fun sendTradeMenuMessage() {
        val dockedTo = ship.dockedToPort
        if (dockedTo != null) {
            val dockedToStation = dockedTo.station
            val stationInfo = dockedToStation.createdShopMessage();
            val myMessage = createMessage("trade_menu_info")
            myMessage.put("station_info", stationInfo)
            myMessage.put("player_balance", account.balance)
            myMessage.put("hold_space", ship.holdCapacity - ship.holdOccupied())
            val holdInfo: JSONObject = ship.createHoldStatusMessage()
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

    fun sendShipUndocked(shipUndockedMessage: JSONObject) {
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
        message.put("ship_id", ship.uuid.toString())
        return message
    }

    fun sendMessage(message: JSONObject) {
        val messageStr = message.toString()
        connection.send(messageStr)
    }

}