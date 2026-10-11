package org.cyuCBMclean.cyuquizReload.platform

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class PlatformTaskRunner(private val plugin: Plugin) : QuizTaskRunner {
    override fun isOwnedByCurrentThread(player: Player): Boolean = Bukkit.isPrimaryThread()

    override fun runEntity(player: Player, task: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            task.run()
        } else {
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    override fun runGlobal(task: Runnable) {
        if (Bukkit.isPrimaryThread()) {
            task.run()
        } else {
            Bukkit.getScheduler().runTask(plugin, task)
        }
    }

    override fun runGlobalLater(delayTicks: Long, task: Runnable): CancellableTask {
        val bukkitTask = Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks)
        return object : CancellableTask {
            override fun cancel() {
                bukkitTask.cancel()
            }
        }
    }

    override fun runGlobalTimer(delayTicks: Long, periodTicks: Long, task: Runnable): CancellableTask {
        val bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks)
        return object : CancellableTask {
            override fun cancel() {
                bukkitTask.cancel()
            }
        }
    }
}
