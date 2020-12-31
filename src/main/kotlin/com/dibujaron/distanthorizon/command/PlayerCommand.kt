package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.player.Player

enum class PlayerCommand(val commandStr: String, private val handler: CommandHandler) {
    HELP("help", HelpCommandHandler()),
    LIST("list", ListCommandHandler()),
    TOP("top", TopCommandHandler()),
    WHOIS("whois", WhoisCommandHandler());
    fun handle(player: Player, args: List<String>) {
        handler.handle(player, args)
    }
}