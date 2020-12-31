package com.dibujaron.distanthorizon.timer

import java.util.concurrent.ConcurrentHashMap

object ScheduledTaskManager {

    private val tasks = ConcurrentHashMap<Int, ScheduledTask>()
    fun tick() {
        tasks.values.asSequence()
            .filter { it.shouldTrigger() }
            .onEach { it.onTrigger() }
            .filter { it.isComplete() }
            .forEach { tasks.remove(it.id) }
    }

    fun submitTask(task: ScheduledTask) {
        tasks[task.id] = task
    }

    fun runDelayed(delay: Int, task: () -> Unit, onCancel: ((CancellationReason) -> Unit)? = null) {
        submitTask(DelayedTask(delay, task, onCancel))
    }

    fun cancelTask(task: ScheduledTask) {
        tasks.remove(task.id)
    }

    private var idCounter = 0
    fun nextId(): Int {
        return idCounter++
    }
}