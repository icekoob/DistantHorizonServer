package com.dibujaron.distanthorizon.command

interface CommandSender {
    fun sendMessage(message: String)
    fun hasPermission(permission: Permission): Boolean
    fun canExecute(command: Command): Boolean {
        return command.canBeExecutedBy(this)
    }
}