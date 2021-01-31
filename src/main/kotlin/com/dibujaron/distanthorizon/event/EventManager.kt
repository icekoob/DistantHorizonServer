package com.dibujaron.distanthorizon.event

import java.util.*

object EventManager {

    private val HANDLERS: MutableList<EventHandler> = LinkedList()

    fun triggerPlayerChatEvent(event: PlayerChatEvent) {
        HANDLERS.forEach { it.onPlayerChat(event) }
    }

    fun registerEvents(handler: EventHandler) {
        HANDLERS.add(handler)
    }
}
