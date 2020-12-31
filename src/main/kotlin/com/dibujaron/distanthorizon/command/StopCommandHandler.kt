package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.DHServer

class StopCommandHandler : CommandHandler {
    override fun handle(sender: CommandSender, args: List<String>) {
        DHServer.shutDown()
        sender.sendMessage("Server shutdown initiated.")
    }

    override fun getRequiredPermissions(): List<Permission> {
        return listOf(Permission.RESTART)
    }
}