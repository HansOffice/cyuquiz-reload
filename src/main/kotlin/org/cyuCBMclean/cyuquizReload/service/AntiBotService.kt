package org.cyuCBMclean.cyuquizReload.service

import org.bukkit.Statistic
import org.bukkit.entity.Player
import org.cyuCBMclean.cyuquizReload.config.AntiBotSettings
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

sealed class AntiBotResult {
    data class Allowed(val resolvedChoice: String) : AntiBotResult()
    data class TooFast(val latency: Long, val minReactionMs: Long) : AntiBotResult()
    data class IpLimit(val maxPerIp: Int) : AntiBotResult()
    data class PenaltyCooldown(val remainingSeconds: Long) : AntiBotResult()
    data class PlaytimeLimit(val minMinutes: Long) : AntiBotResult()
    object InvalidToken : AntiBotResult()
    object Disallowed : AntiBotResult()
}

class AntiBotService(private var settings: AntiBotSettings) {
    private val random = SecureRandom()
    private val questionBroadcastTime = AtomicLong(0L)
    private val answeredIpsThisRound = ConcurrentHashMap<String, Int>()
    private val tokenToOption = ConcurrentHashMap<String, String>()
    private val optionToToken = ConcurrentHashMap<String, String>()
    private val consecutiveWrong = ConcurrentHashMap<UUID, Int>()
    private val penaltyCooldowns = ConcurrentHashMap<UUID, Long>()

    fun updateSettings(newSettings: AntiBotSettings) {
        settings = newSettings
    }

    fun onNewQuestionRound(questionId: String, options: Set<String>) {
        questionBroadcastTime.set(System.currentTimeMillis())
        answeredIpsThisRound.clear()
        tokenToOption.clear()
        optionToToken.clear()

        if (settings.enabled && settings.dynamicToken) {
            for (option in options) {
                val token = "tk_" + generateRandomString(6)
                tokenToOption[token] = option
                optionToToken[option] = token
            }
        }
    }

    fun getTokenForOption(option: String): String? {
        if (!settings.enabled || !settings.dynamicToken) return null
        return optionToToken[option]
    }

    fun verifyAnswer(player: Player, rawInput: String, isChat: Boolean): AntiBotResult {
        if (!settings.enabled) {
            return AntiBotResult.Allowed(rawInput)
        }

        val now = System.currentTimeMillis()
        val latency = now - questionBroadcastTime.get()

        if (latency < settings.minReactionMs) {
            return AntiBotResult.TooFast(latency, settings.minReactionMs)
        }

        val penaltyEnd = penaltyCooldowns[player.uniqueId] ?: 0L
        if (now < penaltyEnd) {
            val remaining = (penaltyEnd - now + 999L) / 1000L
            return AntiBotResult.PenaltyCooldown(remaining)
        }

        if (settings.minPlaytimeMinutes > 0L) {
            val playTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE)
            val minutes = playTicks / 20L / 60L
            if (minutes < settings.minPlaytimeMinutes) {
                return AntiBotResult.PlaytimeLimit(settings.minPlaytimeMinutes)
            }
        }

        if (settings.maxAnswersPerIp > 0) {
            val ip = player.address?.address?.hostAddress ?: "127.0.0.1"
            val count = answeredIpsThisRound.getOrDefault(ip, 0)
            if (count >= settings.maxAnswersPerIp) {
                return AntiBotResult.IpLimit(settings.maxAnswersPerIp)
            }
        }

        val resolved = if (settings.dynamicToken) {
            if (rawInput.startsWith("tk_")) {
                val option = tokenToOption[rawInput] ?: return AntiBotResult.InvalidToken
                option
            } else if (isChat) {
                rawInput
            } else {
                return AntiBotResult.InvalidToken
            }
        } else {
            rawInput
        }

        return AntiBotResult.Allowed(resolved)
    }

    fun recordAnswerSubmitted(player: Player) {
        if (!settings.enabled || settings.maxAnswersPerIp <= 0) return
        val ip = player.address?.address?.hostAddress ?: "127.0.0.1"
        answeredIpsThisRound.compute(ip) { _, count -> (count ?: 0) + 1 }
    }

    fun recordCorrect(uuid: UUID) {
        consecutiveWrong.remove(uuid)
    }

    fun recordWrong(uuid: UUID) {
        if (!settings.enabled || settings.wrongAnswerPenaltySeconds <= 0L) return
        val count = (consecutiveWrong[uuid] ?: 0) + 1
        consecutiveWrong[uuid] = count
        if (count >= settings.wrongAnswerThreshold) {
            penaltyCooldowns[uuid] = System.currentTimeMillis() + settings.wrongAnswerPenaltySeconds * 1000L
            consecutiveWrong.remove(uuid)
        }
    }

    fun clearPlayer(uuid: UUID) {
        consecutiveWrong.remove(uuid)
        penaltyCooldowns.remove(uuid)
    }

    private fun generateRandomString(length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val tokenBuilder = StringBuilder(length)
        repeat(length) {
            tokenBuilder.append(chars[random.nextInt(chars.length)])
        }
        return tokenBuilder.toString()
    }
}
