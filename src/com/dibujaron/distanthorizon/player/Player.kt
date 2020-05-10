package com.dibujaron.distanthorizon.player

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipManager
import com.dibujaron.distanthorizon.ship.ShipClassManager
import io.javalin.websocket.WsContext
import org.json.JSONObject
import java.util.*

class Player(val connection: WsContext) {
    val uuid: UUID = UUID.randomUUID()
    val myShip: Ship = Ship(ShipClassManager.getShipClass("rijay.mockingbird")!!, Vector2(0, 0), Vector2(375, 3180), 0.0)
    var balance = 1000.0
    init {
        ShipManager.markForAdd(myShip)
    }

    fun onMessageFromClient(message: JSONObject) {
        //so far we only have one client message, and it's inputs.
        val messageType = message.getString("message_type")
        if (messageType == "ship_inputs") {
            myShip.mainEnginesActive = message.getBoolean("main_engines_pressed");
            myShip.portThrustersActive = message.getBoolean("port_thrusters_pressed")
            myShip.stbdThrustersActive = message.getBoolean("stbd_thrusters_pressed")
            myShip.foreThrustersActive = message.getBoolean("fore_thrusters_pressed")
            myShip.aftThrustersActive = message.getBoolean("aft_thrusters_pressed")
            myShip.rotatingLeft = message.getBoolean("rotate_left_pressed")
            myShip.rotatingRight = message.getBoolean("rotate_right_pressed")
        } else if(messageType == "attempt_dock") {
            myShip.dockOrUndock()
            sendDockedMessage()
        } else if(messageType == "purchase_from_station") {
            val dockedTo = myShip.dockedToPort
            if(dockedTo != null) {
                val station = dockedTo.station
                val commodity = message.getString("commodity_name")
                val quantity = message.getInt("quantity")
                station.sellResourceToPlayer(commodity, this, myShip, quantity)
                sendDockedMessage()
            }
        } else if(messageType == "sell_to_station") {
            val dockedTo = myShip.dockedToPort
            if(dockedTo != null) {
                val station = dockedTo.station
                val commodity = message.getString("commodity_name")
                val quantity = message.getInt("quantity")
                station.buyResourceFromPlayer(commodity, this, myShip, quantity)
                sendDockedMessage()
            }
        }
    }

    fun isDocked(): Boolean
    {
        return myShip.dockedToPort != null
    }
    fun sendDockedMessage()
    {
        val dockedTo = myShip.dockedToPort
        if(dockedTo != null){
            val dockedToStation = dockedTo.station
            val stationInfo = dockedToStation.createdShopMessage();
            val myMessage = createMessage("docked_to_station")
            myMessage.put("station_info", stationInfo)
            myMessage.put("player_balance", balance)
            myMessage.put("hold_space", myShip.holdCapacity - myShip.holdOccupied())
            val holdInfo: JSONObject = myShip.createHoldStatusMessage()
            myMessage.put("hold_contents", holdInfo)
            sendMessage(myMessage)
        }
    }
    fun sendWorldState(worldStateMessage: JSONObject) {
        val myMessage = createMessage("world_state")
        myMessage.put("world_state", worldStateMessage)
        sendMessage(myMessage)
    }

    fun createMessage(type: String): JSONObject{
        val message = JSONObject();
        message.put("message_type", type)
        message.put("player_id", uuid.toString())
        message.put("ship_id", myShip.uuid.toString())
        return message
    }

    fun sendMessage(message: JSONObject){
        val messageStr = message.toString()
        connection.send(messageStr)
    }

}