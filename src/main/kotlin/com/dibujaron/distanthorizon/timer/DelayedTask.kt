package com.dibujaron.distanthorizon.timer

class DelayedTask(delay: Int, task: () -> Unit, onCancel: ((CancellationReason) -> Unit)? = null) :
    RepeatInvocationTask(
        delay = delay,
        repetitions = 1,
        task = { task.invoke() },
        onCancel = onCancel
    )