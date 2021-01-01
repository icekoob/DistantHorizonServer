package com.dibujaron.distanthorizon.command

import com.dibujaron.distanthorizon.timer.CancellationReason
import com.dibujaron.distanthorizon.timer.ScheduledTaskManager

class CancelRestartCommandHandler : CommandHandler {
    override fun handle(sender: CommandSender, args: List<String>) {
        ScheduledTaskManager.cancelTasksByName(
            RestartCommandHandler.TASK_NAME,
            CancellationReason.CANCELLED_BY_SUBMITTER
        )
        sender.sendMessage("All pending restarts cancelled.")
    }

    override fun getRequiredPermissions(): List<Permission> {
        return listOf(Permission.RESTART)
    }
}