package com.dibujaron.distanthorizon.timer

import com.dibujaron.distanthorizon.DHServer

open class RunUntilCancelledTask(
    private val delay: Int,
    private val initialDelay: Int = delay,
    private val task: (repetition: Int) -> Unit,
    onCancel: ((CancellationReason) -> Unit)? = null,
) : CancellableTask(onCancel) {

    var repetition = 0
    var lastInvocationTime = DHServer.getTickCount()
    override fun onTrigger() {
        if (!shouldTrigger()) {
            throw IllegalStateException("shouldTrigger is false!")
        }
        task.invoke(repetition)
        repetition++
        lastInvocationTime = DHServer.getTickCount()
    }

    override fun shouldTrigger(): Boolean {
        var threshold = initialDelay
        if (repetition != 0) {
            threshold += (repetition - 1) * delay
        }
        return DHServer.getTickCount() == lastInvocationTime + threshold
    }

    override fun isComplete(): Boolean {
        return false
    }
}