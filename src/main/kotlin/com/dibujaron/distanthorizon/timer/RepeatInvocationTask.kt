package com.dibujaron.distanthorizon.timer

open class RepeatInvocationTask(
    taskName: String,
    val delay: Int,
    val initialDelay: Int = 0,
    private val repetitions: Int,
    val task: (repetition: Int) -> Unit,
    onCancel: ((CancellationReason) -> Unit)? = null,

    ) : CancellableTask(taskName, onCancel) {

    var repetition = 0
    var lastInvocationTime = ScheduledTaskManager.getTicks()

    override fun onTrigger() {
        if (!shouldTrigger()) {
            throw IllegalStateException("shouldTrigger is false!")
        }
        task.invoke(repetition)
        repetition++
        lastInvocationTime = ScheduledTaskManager.getTicks()
    }

    override fun shouldTrigger(): Boolean {
        val threshold = if(repetition == 0){
            initialDelay
        } else {
            delay
        }
        return ScheduledTaskManager.getTicks() == lastInvocationTime + threshold
    }

    override fun isComplete(): Boolean {
        return repetition >= repetitions
    }
}