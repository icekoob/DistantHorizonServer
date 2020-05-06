package com.dibujaron.distanthorizon.player

import com.dibujaron.distanthorizon.Vector2
import com.dibujaron.distanthorizon.ship.Ship
import com.dibujaron.distanthorizon.ship.ShipManager
import com.dibujaron.distanthorizon.ship.ShipClass
import com.dibujaron.distanthorizon.ship.ShipClassManager
import io.javalin.websocket.WsContext
import org.json.JSONObject
import java.util.*

class Player(val connection: WsContext) {
    val uuid: UUID = UUID.randomUUID()
    val myShip: Ship = Ship(ShipClassManager.getShipClass("rijay.mockingbird")!!, Vector2(0, 0), Vector2(375, 3180), 0.0)

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
            myShip.attemptDock()
        } else {
            println("Error unknown message type $messageType from player $uuid");
        }
    }

    fun sendWorldState(worldStateMessage: JSONObject) {
        val myMessage = JSONObject()
        myMessage.put("player_id", uuid.toString())
        myMessage.put("ship_id", myShip.uuid.toString())
        myMessage.put("world_state", worldStateMessage)
        val messageStr = myMessage.toString()
        //println(messageStr)
        connection.send(messageStr)
    }

}