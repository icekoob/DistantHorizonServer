package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.player.Player

class TopCommandHandler : CommandHandler {

    override fun handle(sender: Player, args: List<String>) {
        val limit = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 10 else 10
        sender.queueChatMsg("Top $limit players: ")
        DHServer.getDatabase().getPersistenceDatabase().getWealthiestActors(limit)
            .forEach { sender.queueChatMsg(it.displayName + ": $" + it.balance) }
    }
}