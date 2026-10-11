package org.cyuCBMclean.cyuquizReload.util

import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.cyuCBMclean.cyuquizReload.platform.QuizTaskRunner

object VisualUtil {
    @Volatile
    private var currentBossBar: BossBar? = null

    fun sendTitle(player: Player, title: String, subtitle: String, fadeIn: Int, stay: Int, fadeOut: Int) {
        player.sendTitle(MessageUtil.color(title), MessageUtil.color(subtitle), fadeIn, stay, fadeOut)
    }

    @Synchronized
    fun showBossBar(
        taskRunner: QuizTaskRunner,
        title: String,
        color: BarColor,
        style: BarStyle,
        durationTicks: Long,
        players: Collection<Player>,
        batchSize: Int,
        batchDelayTicks: Long
    ) {
        currentBossBar?.removeAll()
        val bar = Bukkit.createBossBar(MessageUtil.color(title), color, style)
        currentBossBar = bar
        players.chunked(batchSize.coerceAtLeast(1)).forEachIndexed { index, batch ->
            val delay = (index * batchDelayTicks.coerceAtLeast(1L)).coerceAtLeast(1L)
            taskRunner.runGlobalLater(delay, Runnable {
                if (currentBossBar != bar) return@Runnable
                for (player in batch) {
                    if (!player.isOnline) continue
                    taskRunner.runEntity(player, Runnable {
                        if (currentBossBar == bar) {
                            bar.addPlayer(player)
                        }
                    })
                }
            })
        }
        taskRunner.runGlobalLater(durationTicks, Runnable {
            if (currentBossBar == bar) {
                bar.removeAll()
                currentBossBar = null
            }
        })
    }

    @Synchronized
    fun removePlayer(player: Player) {
        currentBossBar?.removePlayer(player)
    }

    @Synchronized
    fun hideBossBar() {
        currentBossBar?.removeAll()
        currentBossBar = null
    }
}
