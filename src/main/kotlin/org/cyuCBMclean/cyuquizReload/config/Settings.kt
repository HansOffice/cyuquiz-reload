package org.cyuCBMclean.cyuquizReload.config

import org.bukkit.Sound
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.plugin.java.JavaPlugin
import org.cyuCBMclean.cyuquizReload.model.Difficulty
import org.cyuCBMclean.cyuquizReload.model.QuizEventConfig
import org.cyuCBMclean.cyuquizReload.util.Text

enum class FilterMode {
    WHITELIST,
    BLACKLIST
}

data class AntiBotSettings(
    val enabled: Boolean,
    val minReactionMs: Long,
    val dynamicToken: Boolean,
    val maxAnswersPerIp: Int,
    val wrongAnswerThreshold: Int,
    val wrongAnswerPenaltySeconds: Long,
    val minPlaytimeMinutes: Long
)

data class JackpotSettings(
    val enabled: Boolean,
    val moneyIncrement: Double,
    val maxMultiplier: Int
)

data class TieredRewardSettings(
    val enabled: Boolean,
    val maxRanks: Int,
    val ratios: Map<Int, Double>
)

data class RankTier(
    val correct: Int,
    val name: String,
    val coloredName: String = Text.color(name)
)

data class RankSettings(
    val enabled: Boolean,
    val tiers: List<RankTier>
)

data class FeverSettings(
    val enabled: Boolean,
    val threshold: Int,
    val durationSeconds: Long,
    val quizIntervalSeconds: Long,
    val rewardMultiplier: Int
)

data class PacingSettings(
    val minPlayers: Int
)

data class Settings(
    val quizInterval: Long,
    val filterMode: FilterMode,
    val filteredWorlds: Set<String>,
    val useMySQL: Boolean,
    val mysqlUri: String,
    val oneTryPerQuestion: Boolean,
    val broadcastBatchSize: Int,
    val broadcastBatchDelayTicks: Long,
    val useTitle: Boolean,
    val titleMain: String,
    val titleSub: String,
    val useBossBar: Boolean,
    val bossBarTitle: String,
    val bossBarColor: BarColor,
    val bossBarStyle: BarStyle,
    val cannotAnswerSound: Sound?,
    val streakRewards: Map<Int, List<String>>,
    val events: Map<String, QuizEventConfig>,
    val difficulties: Map<String, Difficulty>,
    val antiBot: AntiBotSettings,
    val jackpot: JackpotSettings,
    val tieredRewards: TieredRewardSettings,
    val ranks: RankSettings,
    val fever: FeverSettings,
    val pacing: PacingSettings
) {
    companion object {
        fun load(plugin: JavaPlugin): Settings {
            val config = plugin.config

            val interval = config.getLong("quiz-interval", 1800L).coerceAtLeast(0L)

            val filterModeStr = config.getString("world-filter.mode", "BLACKLIST")!!.uppercase()
            val filterMode = try {
                FilterMode.valueOf(filterModeStr)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Unknown world-filter mode '$filterModeStr' in config.yml, expected BLACKLIST or WHITELIST")
            }
            val filteredWorlds = config.getStringList("world-filter.worlds").map { it.lowercase() }.toSet()

            val useMySQL = config.getBoolean("database.use-mysql", false)
            val mysqlUri = config.getString("database.uri", "") ?: ""

            val oneTryPerQuestion = config.getBoolean("anti-spam.one-try", true)

            val broadcastBatchSize = config.getInt("performance.broadcast-batch-size", 8).coerceIn(1, 100)
            val broadcastBatchDelayTicks = config.getLong("performance.broadcast-batch-delay-ticks", 1L).coerceAtLeast(1L)

            val useTitle = config.getBoolean("visuals.title.enable", false)
            val titleMain = config.getString("visuals.title.main", "") ?: ""
            val titleSub = config.getString("visuals.title.sub", "") ?: ""

            val useBossBar = config.getBoolean("visuals.bossbar.enable", false)
            val bossBarTitle = config.getString("visuals.bossbar.title", "") ?: ""
            val bossBarColorStr = config.getString("visuals.bossbar.color", "BLUE")!!.uppercase()
            val bossBarColor = try {
                BarColor.valueOf(bossBarColorStr)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Unknown bossbar color '$bossBarColorStr' in config.yml")
            }
            val bossBarStyleStr = config.getString("visuals.bossbar.style", "SOLID")!!.uppercase()
            val bossBarStyle = try {
                BarStyle.valueOf(bossBarStyleStr)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Unknown bossbar style '$bossBarStyleStr' in config.yml")
            }

            val antiBot = AntiBotSettings(
                enabled = config.getBoolean("anti-bot.enabled", true),
                minReactionMs = config.getLong("anti-bot.min-reaction-ms", 800L).coerceAtLeast(0L),
                dynamicToken = config.getBoolean("anti-bot.dynamic-token", true),
                maxAnswersPerIp = config.getInt("anti-bot.max-answers-per-ip", 1).coerceAtLeast(0),
                wrongAnswerThreshold = config.getInt("anti-bot.wrong-answer-threshold", 3).coerceAtLeast(1),
                wrongAnswerPenaltySeconds = config.getLong("anti-bot.wrong-answer-penalty-seconds", 30L).coerceAtLeast(0L),
                minPlaytimeMinutes = config.getLong("anti-bot.min-playtime-minutes", 0L).coerceAtLeast(0L)
            )

            val jackpot = JackpotSettings(
                enabled = config.getBoolean("jackpot.enabled", true),
                moneyIncrement = config.getDouble("jackpot.money-increment", 200.0).coerceAtLeast(0.0),
                maxMultiplier = config.getInt("jackpot.max-multiplier", 5).coerceAtLeast(1)
            )

            val tieredRatios = mutableMapOf<Int, Double>()
            val ratioSection = config.getConfigurationSection("tiered-rewards.ratios")
            if (ratioSection != null) {
                for (rankKey in ratioSection.getKeys(false)) {
                    val rankNumber = rankKey.toIntOrNull() ?: continue
                    tieredRatios[rankNumber] = (ratioSection.getDouble(rankKey, 100.0) / 100.0).coerceIn(0.01, 10.0)
                }
            }
            if (tieredRatios.isEmpty()) {
                tieredRatios[1] = 1.0
                tieredRatios[2] = 0.6
                tieredRatios[3] = 0.3
            }
            val tieredRewards = TieredRewardSettings(
                enabled = config.getBoolean("tiered-rewards.enabled", true),
                maxRanks = config.getInt("tiered-rewards.max-ranks", 3).coerceIn(1, 10),
                ratios = tieredRatios
            )

            val rankTiers = mutableListOf<RankTier>()
            val tierList = config.getMapList("ranks.tiers")
            for (map in tierList) {
                val req = (map["correct"] as? Number)?.toInt() ?: continue
                val name = map["name"]?.toString() ?: continue
                rankTiers.add(RankTier(req, name))
            }
            if (rankTiers.isEmpty()) {
                rankTiers.add(RankTier(0, "&7[童生]"))
                rankTiers.add(RankTier(10, "&a[秀才]"))
                rankTiers.add(RankTier(50, "&b[举人]"))
                rankTiers.add(RankTier(100, "&e[进士]"))
                rankTiers.add(RankTier(200, "&6[探花]"))
                rankTiers.add(RankTier(350, "&c[榜眼]"))
                rankTiers.add(RankTier(500, "&d&l[状元]"))
            }
            rankTiers.sortBy { it.correct }
            val ranks = RankSettings(
                enabled = config.getBoolean("ranks.enabled", true),
                tiers = rankTiers
            )

            val fever = FeverSettings(
                enabled = config.getBoolean("fever.enabled", true),
                threshold = config.getInt("fever.threshold", 20).coerceAtLeast(1),
                durationSeconds = config.getLong("fever.duration-seconds", 180L).coerceAtLeast(10L),
                quizIntervalSeconds = config.getLong("fever.quiz-interval-seconds", 30L).coerceAtLeast(5L),
                rewardMultiplier = config.getInt("fever.reward-multiplier", 2).coerceAtLeast(1)
            )

            val pacing = PacingSettings(
                minPlayers = config.getInt("pacing.min-players", 1).coerceAtLeast(0)
            )

            val streakRewards = mutableMapOf<Int, List<String>>()
            val streakSec = config.getConfigurationSection("streaks")
            if (streakSec != null) {
                for (key in streakSec.getKeys(false)) {
                    val count = key.toIntOrNull() ?: continue
                    streakRewards[count] = streakSec.getStringList(key)
                }
            }

            val events = mutableMapOf<String, QuizEventConfig>()
            val eventSec = config.getConfigurationSection("events")
            if (eventSec != null) {
                for (id in eventSec.getKeys(false)) {
                    val sec = eventSec.getConfigurationSection(id) ?: continue
                    val rewardSec = sec.getConfigurationSection("rewards")
                    val topRewards = mutableMapOf<Int, List<String>>()
                    val topSec = rewardSec?.getConfigurationSection("top")
                    if (topSec != null) {
                        for (rankKey in topSec.getKeys(false)) {
                            val rank = rankKey.toIntOrNull() ?: continue
                            topRewards[rank] = topSec.getStringList(rankKey)
                        }
                    }
                    val difficultyCounts = mutableMapOf<String, Int>()
                    val poolSec = sec.getConfigurationSection("pool.difficulties")
                    if (poolSec != null) {
                        for (difficultyKey in poolSec.getKeys(false)) {
                            val count = poolSec.getInt(difficultyKey, 0)
                            if (count > 0) difficultyCounts[difficultyKey] = count
                        }
                    }
                    val bankIds = sec.getStringList("pool.banks").map { it.lowercase() }.toSet()
                    events[id] = QuizEventConfig(
                        id = id,
                        enabled = sec.getBoolean("enabled", false),
                        name = sec.getString("name", id) ?: id,
                        worlds = sec.getStringList("worlds").map { it.lowercase() }.toSet(),
                        startTime = sec.getString("start-time", "19:00") ?: "19:00",
                        endTime = sec.getString("end-time", "19:30") ?: "19:30",
                        questionCount = sec.getInt("question-count", 15).coerceAtLeast(1),
                        questionIntervalSeconds = sec.getLong("question-interval-seconds", 60L).coerceAtLeast(1L),
                        answerSeconds = sec.getLong("answer-seconds", 45L).coerceAtLeast(1L),
                        difficultyCounts = difficultyCounts,
                        participationRewards = rewardSec?.getStringList("participation").orEmpty(),
                        topRewards = topRewards,
                        bankIds = bankIds
                    )
                }
            }

            val difficulties = mutableMapOf<String, Difficulty>()
            val diffSection = config.getConfigurationSection("difficulties")
                ?: throw IllegalArgumentException("Missing difficulties section in config.yml")
            for (difficultyKey in diffSection.getKeys(false)) {
                val difficultySection = diffSection.getConfigurationSection(difficultyKey) ?: continue
                difficulties[difficultyKey] = Difficulty.fromConfig(difficultyKey, difficultySection)
            }
            require(difficulties.isNotEmpty()) { "At least one difficulty must be defined in config.yml" }

            val cannotAnswerSound = Sound.valueOf("ENTITY_VILLAGER_NO")

            return Settings(
                quizInterval = interval,
                filterMode = filterMode,
                filteredWorlds = filteredWorlds,
                useMySQL = useMySQL,
                mysqlUri = mysqlUri,
                oneTryPerQuestion = oneTryPerQuestion,
                broadcastBatchSize = broadcastBatchSize,
                broadcastBatchDelayTicks = broadcastBatchDelayTicks,
                useTitle = useTitle,
                titleMain = titleMain,
                titleSub = titleSub,
                useBossBar = useBossBar,
                bossBarTitle = bossBarTitle,
                bossBarColor = bossBarColor,
                bossBarStyle = bossBarStyle,
                cannotAnswerSound = cannotAnswerSound,
                streakRewards = streakRewards,
                events = events,
                difficulties = difficulties,
                antiBot = antiBot,
                jackpot = jackpot,
                tieredRewards = tieredRewards,
                ranks = ranks,
                fever = fever,
                pacing = pacing
            )
        }
    }
}
