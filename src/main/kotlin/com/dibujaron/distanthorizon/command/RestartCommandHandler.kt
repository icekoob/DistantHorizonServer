package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.timer.RepeatWarningExecutionTask
import com.dibujaron.distanthorizon.timer.ScheduledTaskManager

class RestartCommandHandler : CommandHandler {

    companion object{
        val TASK_NAME = "delayedRestartTask"
    }

    override fun handle(sender: CommandSender, args: List<String>) {
        var timeInSeconds = 120
        var warnIntervalSeconds = 10
        if (args.isNotEmpty()) {
            timeInSeconds = args[0].toIntOrNull() ?: 120
            if (args.size > 1) {
                warnIntervalSeconds = args[1].toIntOrNull() ?: 10
            }
        }
        sender.sendMessage("Server will restart in $timeInSeconds seconds, with a warning every $warnIntervalSeconds seconds.")
        val timeInTicks = timeInSeconds * DHServer.TICKS_PER_SECOND
        val warnIntervalTicks = DHServer.TICKS_PER_SECOND * warnIntervalSeconds
        val warningCount = timeInTicks / warnIntervalTicks
        var remainingSeconds = timeInSeconds
        val task = RepeatWarningExecutionTask(TASK_NAME,
            warnIntervalTicks,
            warningCount,
            {
                PlayerManager.broadcast("WARNING: The server will restart in $remainingSeconds seconds! Dock at a station to save your progress!")
                remainingSeconds -= warnIntervalSeconds
            },
            {
                PlayerManager.broadcast("WARNING: The server is now shut down, logging out is recommended!")
                DHServer.restart(sender)
            }
        )
        ScheduledTaskManager.submitTask(task)
    }

    override fun getRequiredPermissions(): List<Permission> {
        return listOf(Permission.RESTART)
    }
}