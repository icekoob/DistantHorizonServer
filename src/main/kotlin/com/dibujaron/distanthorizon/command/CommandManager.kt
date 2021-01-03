package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.Player

object CommandManager {

    val CONSOLE_SENDER = ConsoleCommandSender()
    fun handlePlayerCommand(issuer: Player, commandStr: String): Boolean {
        if (commandStr.startsWith("/")) {
            println("handling command $commandStr from player ${issuer.getUsername()}")
            val commandAndArgs = commandStr.substring(1)
            return handle(issuer, commandAndArgs)
        }
        return false;
    }

    fun handleConsoleCommand(commandStr: String): Boolean {
        return handle(CONSOLE_SENDER, commandStr)
    }

    private fun handle(sender: CommandSender, commandStr: String): Boolean {
        val commandAndArgs = commandStr.toLowerCase().split(" ")
        if (commandAndArgs.isNotEmpty()) {
            val trueCommandStr = commandAndArgs[0]
            val cmd = Command.values().asSequence()
                .find { it.commandStr == trueCommandStr }
            if (cmd != null && sender.canExecute(cmd)) {
                cmd.handle(sender, commandAndArgs.subList(1, commandAndArgs.size))
                return true;
            }
        }
        return false;
    }
}