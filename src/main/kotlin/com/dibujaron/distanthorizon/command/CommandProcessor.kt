package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.Player

object CommandProcessor {
    fun handlePlayerCommand(issuer: Player, commandStr: String): Boolean {
        if(commandStr.startsWith("/")){
            println("handling command $commandStr from player ${issuer.getUsername()}")
            val commandAndArgs = commandStr.substring(1).toLowerCase().split(" ")
            if(commandAndArgs.isEmpty()){
                return false;
            } else {
                val trueCommandStr = commandAndArgs[0]
                val cmd = PlayerCommand.values().asSequence()
                    .find { it.commandStr == trueCommandStr }
                if (cmd != null) {
                    cmd.handle(issuer, commandAndArgs.subList(1, commandAndArgs.size))
                    return true;
                }
            }
        }
        return false;
    }
}