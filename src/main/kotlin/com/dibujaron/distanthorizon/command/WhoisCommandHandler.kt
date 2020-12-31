package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.Player
import com.dibujaron.distanthorizon.player.PlayerManager

class WhoisCommandHandler : CommandHandler {
    override fun handle(sender: Player, args: List<String>) {
        if (args.isNotEmpty()) {
            val name = args[0]
            PlayerManager.getPlayers()
                .filter { it.accountInfo?.accountName ?: "" == name || it.actorInfo?.displayName ?: "" == name }
                .forEach {
                    sender.queueChatMsg("Account: ${it.accountInfo?.accountName}")
                    sender.queueChatMsg("Character: ${it.actorInfo?.displayName}")
                    sender.queueChatMsg("Balance: ${it.actorInfo?.balance}")
                    sender.queueChatMsg("Ship: ${it.ship.type.manufacturer.displayNameShort} ${it.ship.type.displayName}")
                }
        }
    }
}