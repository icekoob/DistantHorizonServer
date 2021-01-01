package com.dibujaron.distanthorizon.timer

import org.junit.Test

class TimerTest {
    @Test
    fun testDelayedTask() {
        var complete = false
        var startTick = ScheduledTaskManager.getTicks()
        ScheduledTaskManager.submitTask(DelayedTask("testDelayedTask",5, {complete = true}))
        runTaskManagerUntilNoMoreTasks()
        var currentTick = ScheduledTaskManager.getTicks()
        assert(complete)
        assert((currentTick - startTick) == 6)
    }

    @Test
    fun testRepeatingTask() {
        var invocations = 0
        var startTick = ScheduledTaskManager.getTicks()
        ScheduledTaskManager.submitTask(RepeatInvocationTask("testRepeatTask", 5, 0, 5, {invocations++}))
        runTaskManagerUntilNoMoreTasks()
        var currentTick = ScheduledTaskManager.getTicks()
        assert(invocations == 5)
        assert((currentTick - startTick) == 21) //20 ticks to do the whole thing, then +1 to get here.
    }

    private fun runTaskManagerUntilNoMoreTasks() {
        while (ScheduledTaskManager.hasTasks()) {
            ScheduledTaskManager.tick()
        }
    }
}