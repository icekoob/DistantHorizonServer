package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager

class ListCommandHandler : CommandHandler{
    override fun handle(sender: Player, args: List<String>) {
        val players = PlayerManager.getPlayers(false).sortedBy { it.getDisplayName() }.toList()
        sender.queueChatMsg("Current online players: ${players.size}")
        PlayerManager.getPlayers(false)
            .forEach {
                sender.queueChatMsg(it.getDisplayName() + " (" + it.getUsername() + ")")
            }
    }
}