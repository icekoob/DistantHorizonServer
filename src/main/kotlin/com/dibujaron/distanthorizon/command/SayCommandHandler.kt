package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.PlayerManager

class SayCommandHandler : CommandHandler {

    override fun handle(sender: CommandSender, args: List<String>) {
        PlayerManager.broadcast("[CONSOLE]: " + args.joinToString(" "))
    }

    override fun getRequiredPermissions(): List<Permission> {
        return listOf(Permission.RESTART)
    }
}