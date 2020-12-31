package com.dibujaron.distanthorizon.command

interface CommandHandler {
    fun handle(sender: CommandSender, args: List<String>)

    fun getRequiredPermissions(): List<Permission> {
        return emptyList()
    }
}