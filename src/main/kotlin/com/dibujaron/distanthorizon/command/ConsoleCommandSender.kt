package com.dibujaron.distanthorizon.command

class ConsoleCommandSender : CommandSender {
    override fun sendMessage(message: String) {
        println(message)
    }

    override fun hasPermission(permission: Permission): Boolean {
        return true
    }

}