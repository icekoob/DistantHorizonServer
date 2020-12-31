package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.PlayerManager

class WhoisCommandHandler : CommandHandler {
    override fun handle(sender: CommandSender, args: List<String>) {
        if (args.isNotEmpty()) {
            val name = args[0]
            PlayerManager.getPlayers()
                .filter { it.accountInfo?.accountName ?: "" == name || it.actorInfo?.displayName ?: "" == name }
                .forEach {
                    sender.sendMessage("Account: ${it.accountInfo?.accountName}")
                    sender.sendMessage("Character: ${it.actorInfo?.displayName}")
                    sender.sendMessage("Balance: ${it.actorInfo?.balance}")
                    sender.sendMessage("Ship: ${it.ship.type.manufacturer.displayNameShort} ${it.ship.type.displayName}")
                }
        }
    }
}