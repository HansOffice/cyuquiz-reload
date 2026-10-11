package org.cyuCBMclean.cyuquizReload.service

import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.cyuCBMclean.cyuquizReload.platform.QuizTaskRunner
import org.cyuCBMclean.cyuquizReload.util.MessageUtil
import org.cyuCBMclean.cyuquizReload.util.SoundUtil

class RewardService(
    private val plugin: Plugin,
    private val taskRunner: QuizTaskRunner
) {

    fun grantRewards(player: Player, rewards: List<String>, vars: Map<String, String>, multiplier: Double = 1.0) {
        if (rewards.isEmpty()) return
        for (reward in rewards) {
            val parts = reward.split(":", limit = 2)
            if (parts.size < 2) continue

            val type = parts[0].trim().lowercase()
            val rawPayload = parts[1].trim()
            val formatted = MessageUtil.format(rawPayload, vars)
            val scaledPayload = if (multiplier != 1.0) scaleNumericReward(formatted, multiplier) else formatted
            val payload = MessageUtil.setPlaceholders(player, scaledPayload)

            val task = Runnable {
                when (type) {
                    "player" -> Bukkit.dispatchCommand(player, payload)
                    "console" -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), payload)
                    "sound" -> {
                        try {
                            val sound = Sound.valueOf(payload.uppercase())
                            SoundUtil.play(player, sound)
                        } catch (_: IllegalArgumentException) {
                            plugin.logger.warning("无效的奖励音效配置: $payload")
                        }
                    }
                    else -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), payload)
                }
            }

            if (type == "player") {
                taskRunner.runEntity(player, task)
            } else {
                taskRunner.runGlobal(task)
            }
        }
    }

    private fun scaleNumericReward(command: String, multiplier: Double): String {
        val parts = command.split(" ").toMutableList()
        val lastIndex = parts.lastIndex
        if (lastIndex >= 0) {
            val numericAmount = parts[lastIndex].toDoubleOrNull()
            if (numericAmount != null) {
                val scaled = (numericAmount * multiplier).toLong().coerceAtLeast(1L)
                parts[lastIndex] = scaled.toString()
                return parts.joinToString(" ")
            }
        }
        return command
    }
}