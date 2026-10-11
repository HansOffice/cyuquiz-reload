package org.cyuCBMclean.cyuquizReload.platform

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class PlatformTaskRunner(private val plugin: Plugin) : QuizTaskRunner {
    override fun isOwnedByCurrentThread(player: Player): Boolean = Bukkit.isOwnedByCurrentRegion(player)

    override fun runEntity(player: Player, task: Runnable) {
        if (isOwnedByCurrentThread(player)) {
            task.run()
        } else {
            player.scheduler.run(plugin, { task.run() }, null)
        }
    }

    override fun runGlobal(task: Runnable) {
        Bukkit.getGlobalRegionScheduler().run(plugin) { task.run() }
    }

    override fun runGlobalLater(delayTicks: Long, task: Runnable): CancellableTask {
        val scheduledTask = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, { task.run() }, delayTicks.coerceAtLeast(1L))
        return object : CancellableTask {
            override fun cancel() {
                scheduledTask.cancel()
            }
        }
    }

    override fun runGlobalTimer(delayTicks: Long, periodTicks: Long, task: Runnable): CancellableTask {
        val scheduledTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, { task.run() }, delayTicks.coerceAtLeast(1L), periodTicks.coerceAtLeast(1L))
        return object : CancellableTask {
            override fun cancel() {
                scheduledTask.cancel()
            }
        }
    }
}
