package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.Player

class HelpCommandHandler : CommandHandler{
    override fun handle(sender: Player, args: List<String>) {
        sender.queueChatMsg("All available commands: ")
        PlayerCommand.values().asSequence().forEach {
            sender.queueChatMsg("/" + it.commandStr)
        }
    }
}