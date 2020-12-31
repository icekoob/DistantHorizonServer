package com.dibujaron.distanthorizon.timer

abstract class CancellableTask(private val onCancel: ((CancellationReason) -> Unit)?) : ScheduledTask() {

    override fun onCancelled(reason: CancellationReason) {
        onCancel?.invoke(reason)
    }
}