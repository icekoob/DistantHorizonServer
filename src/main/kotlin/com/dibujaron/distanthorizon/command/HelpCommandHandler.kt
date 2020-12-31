package com.dibujaron.distanthorizon.command

class HelpCommandHandler : CommandHandler{
    override fun handle(sender: CommandSender, args: List<String>) {
        sender.sendMessage("All available commands: ")
        Command.values().asSequence().forEach {
            sender.sendMessage("/" + it.commandStr)
        }
    }
}