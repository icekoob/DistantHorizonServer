package com.dibujaron.distanthorizon.timer

open class RepeatWarningExecutionTask(
    taskName: String,
    warnInterval: Int,
    warningCount: Int,
    warningTask: (repetition: Int) -> Unit,
    finalTask: () -> Unit,
    onCancel: ((CancellationReason) -> Unit)? = null,

    ) : RepeatInvocationTask(taskName, warnInterval, 0, warningCount + 1, {
    if (it == warningCount) {
        finalTask.invoke()
    } else {
        warningTask.invoke(it)
    }
}, onCancel)