package com.dibujaron.distanthorizon.timer

abstract class CancellableTask(taskName: String, private val onCancel: ((CancellationReason) -> Unit)?) :
    ScheduledTask(taskName) {

    override fun onCancelled(reason: CancellationReason) {
        onCancel?.invoke(reason)
    }
}