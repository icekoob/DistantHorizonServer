package com.dibujaron.distanthorizon.command

enum class Command(val commandStr: String, private val handler: CommandHandler) {
    STOP("stop", StopCommandHandler()),
    HELP("help", HelpCommandHandler()),
    LIST("list", ListCommandHandler()),
    TOP("top", TopCommandHandler()),
    WHOIS("whois", WhoisCommandHandler()),
    RESTART("restart", RestartCommandHandler()),
    CANCEL_RESTART("cancelrestart", CancelRestartCommandHandler());

    fun canBeExecutedBy(sender: CommandSender): Boolean {
        return handler.getRequiredPermissions().all { sender.hasPermission(it) }
    }

    fun handle(sender: CommandSender, args: List<String>) {
        handler.handle(sender, args)
    }
}