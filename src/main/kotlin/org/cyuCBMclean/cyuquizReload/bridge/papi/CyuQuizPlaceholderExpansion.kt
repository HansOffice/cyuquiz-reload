package org.cyuCBMclean.cyuquizReload.bridge.papi

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import org.cyuCBMclean.cyuquizReload.CyuquizReload
import org.cyuCBMclean.cyuquizReload.config.Settings
import org.cyuCBMclean.cyuquizReload.service.QuizService
import org.cyuCBMclean.cyuquizReload.service.StatsService
import java.util.Locale

class CyuQuizPlaceholderExpansion(
    private val plugin: CyuquizReload,
    private val statsService: StatsService,
    private val quizService: QuizService,
    private val settingsSupplier: () -> Settings
) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "cyuquiz"

    override fun getAuthor(): String = plugin.description.authors.firstOrNull() ?: "cyuCBMclean"

    override fun getVersion(): String = plugin.description.version

    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, identifier: String): String {
        if (player == null) return ""
        val stats = statsService.getStats(player.uniqueId)
        val settings = settingsSupplier()

        return when (identifier.lowercase(Locale.ROOT)) {
            "answered" -> stats.answered.toString()
            "correct" -> stats.correct.toString()
            "wrong" -> stats.wrong.toString()
            "current_streak" -> stats.currentStreak.toString()
            "highest_streak" -> stats.highestStreak.toString()
            "accuracy" -> {
                if (stats.answered > 0) {
                    val rate = (stats.correct.toDouble() / stats.answered.toDouble()) * 100.0
                    String.format(Locale.ROOT, "%.1f%%", rate)
                } else {
                    "0.0%"
                }
            }
            "rank", "rank_name" -> {
                val tiers = settings.ranks.tiers
                val tier = tiers.lastOrNull { stats.correct >= it.correct } ?: tiers.firstOrNull()
                tier?.coloredName ?: ""
            }
            "rank_next" -> {
                val tiers = settings.ranks.tiers
                val nextTier = tiers.firstOrNull { stats.correct < it.correct }
                nextTier?.correct?.toString() ?: "MAX"
            }
            "jackpot" -> quizService.currentJackpotMultiplier.toString()
            "fever" -> if (quizService.isFeverActive()) "是" else "否"
            else -> ""
        }
    }
}
