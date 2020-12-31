package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.DHServer

class TopCommandHandler : CommandHandler {

    override fun handle(sender: CommandSender, args: List<String>) {
        val limit = if (args.isNotEmpty()) args[0].toIntOrNull() ?: 10 else 10
        sender.sendMessage("Top $limit players: ")
        DHServer.getDatabase().getPersistenceDatabase().getWealthiestActors(limit)
            .forEach { sender.sendMessage(it.displayName + ": $" + it.balance) }
    }
}