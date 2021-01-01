package com.dibujaron.distanthorizon.timer

abstract class ScheduledTask(val taskName: String) {
    val scheduledAt = ScheduledTaskManager.getTicks()
    val id = ScheduledTaskManager.nextId()


    abstract fun onTrigger()

    abstract fun shouldTrigger(): Boolean

    abstract fun isComplete(): Boolean

    open fun onCancelled(reason: CancellationReason) {

    }

    fun cancel(reason: CancellationReason) {
        ScheduledTaskManager.cancelTask(this, reason)
    }

    fun schedule() {
        ScheduledTaskManager.submitTask(this)
    }

    fun ticksSinceScheduled(): Int {
        return ScheduledTaskManager.getTicks() - scheduledAt
    }
}