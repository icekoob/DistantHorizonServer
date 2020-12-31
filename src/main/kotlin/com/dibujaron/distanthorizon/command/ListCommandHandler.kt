package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.ship.ShipManager

class ListCommandHandler : CommandHandler {
    override fun handle(sender: Player, args: List<String>) {
        val players = PlayerManager.getPlayers(false).sortedBy { it.getDisplayName() }.toList()
        sender.queueChatMsg("Human players on this server: ${players.size}")
        PlayerManager.getPlayers(false)
            .forEach {
                sender.queueChatMsg(it.getDisplayName() + " (" + it.getUsername() + ")")
            }
        sender.queueChatMsg("Total ships currently active: ${ShipManager.getShips().size}")
    }
}