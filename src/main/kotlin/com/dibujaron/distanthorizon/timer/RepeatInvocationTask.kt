package com.dibujaron.distanthorizon.timer

open class RepeatInvocationTask(
    delay: Int,
    initialDelay: Int = 0,
    private val repetitions: Int,
    task: (repetition: Int) -> Unit,
    onCancel: ((CancellationReason) -> Unit)? = null,

    ) : RunUntilCancelledTask(delay, initialDelay, task, onCancel) {

    override fun isComplete(): Boolean {
        return super.repetition >= repetitions
    }
}