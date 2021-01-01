package com.dibujaron.distanthorizon.timer

class DelayedTask(taskName: String, delay: Int, task: () -> Unit, onCancel: ((CancellationReason) -> Unit)? = null) :
    RepeatInvocationTask(
        taskName = taskName,
        delay = delay,
        initialDelay = delay,
        repetitions = 1,
        task = { task.invoke() },
        onCancel = onCancel
    )