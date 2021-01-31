package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.ship.ShipManager

class ListCommandHandler : CommandHandler {
    override fun handle(sender: CommandSender, args: List<String>) {
        val players = PlayerManager.getPlayers(false).sortedBy { it.getDisplayName() }.toList()
        sender.sendMessage("Human players on this server: ${players.size}")
        PlayerManager.getPlayers(false)
            .forEach {
                sender.sendMessage(it.getDisplayName() + " (" + it.getUsername() + ")")
            }
        sender.sendMessage("Total ships currently active: ${ShipManager.getShips().size}")
    }

    override fun getRequiredPermissions(): List<Permission> {
        return listOf(Permission.LIST)
    }
}