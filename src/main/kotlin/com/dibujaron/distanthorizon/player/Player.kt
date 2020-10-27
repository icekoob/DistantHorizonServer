package com.dibujaron.distanthorizon.player

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.*
import io.javalin.websocket.WsContext
import org.json.JSONArray
import org.json.JSONObject
import java.awt.Color
import java.util.*

class Player(val connection: WsContext) {
    val uuid: UUID = UUID.randomUUID()
    val companionAI: PlayerCompanionAI = PlayerCompanionAI(uuid);
    val ship: Ship = Ship(
        ShipClassManager.getShipClass(DHServer.playerStartingShip)!!,
        ShipColor(Color(128, 128, 128)),//ShipColor(Color(0,148,255)),
        ShipColor(Color(205, 106, 0)),
        ShipState(Vector2(375, 3180), 0.0, Vector2.ZERO),
        true
    )
    val incomingMessageQueue: Queue<JSONObject> = LinkedList()
    val outgoingMessageQueue: Queue<JSONObject> = LinkedList()

    var account = Account()

    init {
        println("initializing player with default starting ship ${DHServer.playerStartingShip}")
        ShipManager.markForAdd(ship)
        queueChatMsg("... Shipboard artificial intelligence is loading ...")
        queueShipAIChatMsg(companionAI.getInitializationMessage())
        queueShipAIChatMsg(companionAI.getGreeting())
    }

    fun queueIncomingMessageFromClient(message: JSONObject) {
        if(DHServer.REQUEST_BATCHING) {
            incomingMessageQueue.add(message)
        } else {
            processIncomingMessage(message)
        }
    }

    //todo batching
    fun tick(){
        while(!incomingMessageQueue.isEmpty()){
            processIncomingMessage(incomingMessageQueue.remove())
        }
        while(!outgoingMessageQueue.isEmpty()){
            sendMessage(outgoingMessageQueue.remove())
        }
    }

    private fun processIncomingMessage(message: JSONObject){
        //todo refactor - separate handlers for each message type?
        val messageType = message.getString("message_type")
        if (messageType == "ship_inputs") {
            val inputs = ShipInputs(message)
            ship.receiveInputChange(inputs)
        } else if (messageType == "dock") {
            if (!ship.isDocked()) {
                when (ship.attemptDock()) {
                    DockingResult.SUCCESS -> queueSendStationMenuMessage()
                    DockingResult.DISTANCE_TOO_GREAT -> queueShipAIChatMsg("We cannot dock; no stations are within docking range.")
                    DockingResult.CLOSING_SPEED_TOO_GREAT -> queueShipAIChatMsg("Our relative velocity is too high for the docking magnets to safely engage.")
                }
            }
        } else if (messageType == "undock") {
            if (ship.isDocked()) {
                ship.undock()
                queueSendStationMenuMessage()
            } else {
                queueChatMsg("Ship is not docked?")
            }
        } else if (messageType == "purchase_from_station") {
            if (ship.isDocked()) {
                val commodity = message.getString("commodity_name")
                val quantity = message.getInt("quantity")
                ship.buyResourceFromStation(commodity, account, quantity)
                println("bought $quantity of $commodity from station, new balance is ${account.balance}")
                queueSendStationMenuMessage()
            }
        } else if (messageType == "sell_to_station") {
            if (ship.isDocked()) {
                val commodity = message.getString("commodity_name")
                val quantity = message.getInt("quantity")
                ship.sellResourceToStation(commodity, account, quantity)
                println("sold $quantity of $commodity to station, new balance is ${account.balance}")
                queueSendStationMenuMessage()
            }
        } else if (messageType == "chat") {
            val payload = message.getString("payload")
            PlayerManager.broadcast(uuid.toString(), payload)
        }
    }

    private fun queueSendStationMenuMessage() {
        val dockedTo = ship.dockedToPort
        if (dockedTo != null) {
            val dockedToStation = dockedTo.station
            val stationInfo = dockedToStation.createdShopMessage();
            val myMessage = createMessage("station_menu_info")
            myMessage.put("station_info", stationInfo)
            myMessage.put("player_balance", account.balance)
            myMessage.put("hold_space", ship.holdCapacity - ship.holdOccupied())
            val holdInfo: JSONObject = ship.createHoldStatusMessage()
            myMessage.put("hold_contents", holdInfo)
            queueMessage(myMessage)
        } else {
            val myMessage = createMessage("station_menu_close")
            queueMessage(myMessage)
        }
    }

    fun queueWorldStateMsg(worldStateMessage: JSONObject) {
        val myMessage = createMessage("world_state")
        myMessage.put("world_state", worldStateMessage)
        queueMessage(myMessage)
    }

    fun queueShipDockedMsg(shipDockedMessage: JSONObject) {
        val myMessage = createMessage("ship_docked")
        myMessage.put("ship_docked", shipDockedMessage);
        queueMessage(myMessage)
    }

    fun queueShipUndockedMsg(shipUndockedMessage: JSONObject) {
        val myMessage = createMessage("ship_undocked")
        myMessage.put("ship_undocked", shipUndockedMessage);
        queueMessage(myMessage)
    }

    fun queueInputsUpdateMsg(shipInputsUpdate: JSONObject) {
        val myMessage = createMessage("ship_inputs")
        myMessage.put("ship_inputs", shipInputsUpdate)
        queueMessage(myMessage)
    }

    fun queueShipHeartbeatsMsg(shipHeartbeats: JSONArray) {
        val myMessage = createMessage("ship_heartbeats")
        myMessage.put("ship_heartbeats", shipHeartbeats)
        queueMessage(myMessage)
    }

    fun queueShipsAddedMsg(shipsAdded: JSONArray) {
        val myMessage = createMessage("ships_added")
        myMessage.put("ships_added", shipsAdded)
        queueMessage(myMessage)
    }

    fun queueShipsRemovedMsg(shipsRemoved: JSONArray) {
        val myMessage = createMessage("ships_removed")
        myMessage.put("ships_removed", shipsRemoved)
        queueMessage(myMessage)
    }

    fun queueShipAIChatMsg(message: String){
        queueChatMsg("[${companionAI.getName()}]", message)
    }

    fun queueChatMsg(senderName: String, message: String){
        queueChatMsg("$senderName: $message")
    }
    fun queueChatMsg(message: String) {
        val myMessage = createMessage("chat")
        myMessage.put("payload", message)
        queueMessage(myMessage)
    }

    fun createMessage(type: String): JSONObject {
        val message = JSONObject();
        message.put("message_type", type)
        message.put("player_id", uuid.toString())
        message.put("ship_id", ship.uuid.toString())
        return message
    }

    fun queueMessage(message: JSONObject) {
        if(DHServer.REQUEST_BATCHING) {
            outgoingMessageQueue.add(message)
        } else {
            sendMessage(message)
        }
    }

    fun sendMessage(message: JSONObject ){
        val messageStr = message.toString()
        connection.send(messageStr)
    }

}