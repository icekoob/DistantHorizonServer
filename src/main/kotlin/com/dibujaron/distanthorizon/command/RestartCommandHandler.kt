package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.DHServer
import com.dibujaron.distanthorizon.player.PlayerManager
import com.dibujaron.distanthorizon.timer.RepeatWarningExecutionTask
import com.dibujaron.distanthorizon.timer.ScheduledTaskManager

class RestartCommandHandler : CommandHandler {
    override fun handle(sender: CommandSender, args: List<String>) {
        var timeInMinutes = 2
        if (args.isNotEmpty()) {
            timeInMinutes = args[0].toIntOrNull() ?: 2
        }
        sender.sendMessage("Server will restart in $timeInMinutes minutes.")
        val timeInSeconds = timeInMinutes * 60
        val timeInTicks = timeInSeconds * DHServer.TICKS_PER_SECOND
        val warnIntervalTicks = DHServer.TICKS_PER_SECOND * 10
        val warningCount = timeInTicks / warnIntervalTicks
        val task = RepeatWarningExecutionTask(
            warnIntervalTicks,
            warningCount,
            {
                val elapsedTimeTicks = it * warnIntervalTicks
                val elapsedTimeSeconds = elapsedTimeTicks / DHServer.TICK_LENGTH_SECONDS
                val remainingTime = (timeInSeconds - elapsedTimeSeconds).toInt()
                PlayerManager.broadcast("WARNING: The server will restart in $remainingTime seconds! Dock at a station to save your progress!")
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