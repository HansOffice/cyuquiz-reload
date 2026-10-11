package org.cyuCBMclean.cyuquizReload.platform

import org.bukkit.entity.Player

interface CancellableTask {
    fun cancel()
}

interface QuizTaskRunner {
    fun isOwnedByCurrentThread(player: Player): Boolean
    fun runEntity(player: Player, task: Runnable)
    fun runGlobal(task: Runnable)
    fun runGlobalLater(delayTicks: Long, task: Runnable): CancellableTask
    fun runGlobalTimer(delayTicks: Long, periodTicks: Long, task: Runnable): CancellableTask
}
