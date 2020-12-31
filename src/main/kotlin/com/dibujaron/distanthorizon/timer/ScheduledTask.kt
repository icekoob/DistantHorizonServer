package com.dibujaron.distanthorizon.timer

import com.dibujaron.distanthorizon.DHServer

abstract class ScheduledTask {
    val scheduledAt = DHServer.getTickCount()
    val id = ScheduledTaskManager.nextId()


    abstract fun onTrigger()

    abstract fun shouldTrigger(): Boolean

    abstract fun isComplete(): Boolean

    open fun onCancelled(reason: CancellationReason) {

    }

    fun cancel() {
        ScheduledTaskManager.cancelTask(this)
    }

    fun schedule() {
        ScheduledTaskManager.submitTask(this)
    }

    fun ticksSinceScheduled(): Int {
        return DHServer.getTickCount() - scheduledAt
    }
}