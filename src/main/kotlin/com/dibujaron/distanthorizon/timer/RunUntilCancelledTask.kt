package com.dibujaron.distanthorizon.timer

open class RunUntilCancelledTask(
    taskName: String,
    delay: Int,
    initialDelay: Int = 0,
    task: (repetition: Int) -> Unit,
    onCancel: ((CancellationReason) -> Unit)? = null,
) : RepeatInvocationTask(taskName, delay, initialDelay, Integer.MAX_VALUE, task, onCancel) {

    override fun isComplete(): Boolean {
        return false
    }
}