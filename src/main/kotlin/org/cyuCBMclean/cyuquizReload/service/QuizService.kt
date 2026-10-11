package org.cyuCBMclean.cyuquizReload.service

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.cyuCBMclean.cyuquizReload.api.event.QuizPlayerAnswerEvent
import org.cyuCBMclean.cyuquizReload.api.event.QuizQuestionBroadcastEvent
import org.cyuCBMclean.cyuquizReload.config.FilterMode
import org.cyuCBMclean.cyuquizReload.config.MessageManager
import org.cyuCBMclean.cyuquizReload.config.Settings
import org.cyuCBMclean.cyuquizReload.model.EventPlayerStats
import org.cyuCBMclean.cyuquizReload.model.Question
import org.cyuCBMclean.cyuquizReload.model.QuizEventConfig
import org.cyuCBMclean.cyuquizReload.model.QuizType
import org.cyuCBMclean.cyuquizReload.platform.QuizTaskRunner
import org.cyuCBMclean.cyuquizReload.storage.QuestionCatalog
import org.cyuCBMclean.cyuquizReload.util.MessageUtil
import org.cyuCBMclean.cyuquizReload.util.SoundUtil
import org.cyuCBMclean.cyuquizReload.util.VisualUtil
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class QuizService(
    private val plugin: Plugin,
    private var settings: Settings,
    private val messageManager: MessageManager,
    private val questionCatalog: QuestionCatalog,
    private val statsService: StatsService,
    private val rewardService: RewardService,
    private val antiBotService: AntiBotService,
    private val taskRunner: QuizTaskRunner
) {
    @Volatile
    var currentQuestion: Question? = null
        private set

    @Volatile
    var currentJackpotMultiplier: Int = 1
        private set

    @Volatile
    private var feverEndTimeMillis: Long = 0L

    private val globalCorrectCounter = AtomicInteger(0)
    private val answeredThisRound = ConcurrentHashMap.newKeySet<UUID>()
    private val answeredQuestions = ConcurrentHashMap<UUID, MutableSet<String>>()
    private val roundWinners = CopyOnWriteArrayList<UUID>()
    private val answeredCorrectlyThisRound = AtomicBoolean(false)
    private val broadcastGeneration = AtomicLong(0L)

    @Volatile
    private var activeEvent: ActiveQuizEvent? = null
    private val startedEvents = ConcurrentHashMap.newKeySet<String>()

    private data class ActiveQuizEvent(
        val config: QuizEventConfig,
        val startedDate: LocalDate,
        val stats: MutableMap<UUID, EventPlayerStats> = ConcurrentHashMap(),
        val askedQuestions: MutableSet<String> = ConcurrentHashMap.newKeySet(),
        val answeredThisQuestion: MutableSet<UUID> = ConcurrentHashMap.newKeySet(),
        var questionNumber: Int = 0,
        var generation: Long = 0L,
        var answerDeadlineMillis: Long = 0L
    )

    fun updateSettings(newSettings: Settings) {
        settings = newSettings
        antiBotService.updateSettings(newSettings.antiBot)
    }

    fun isEventRunning(): Boolean = activeEvent != null

    fun runningEventName(): String? = activeEvent?.config?.name

    fun isFeverActive(): Boolean = System.currentTimeMillis() < feverEndTimeMillis

    fun isWorldAllowed(worldName: String): Boolean {
        val lower = worldName.lowercase()
        return when (settings.filterMode) {
            FilterMode.WHITELIST -> settings.filteredWorlds.contains(lower)
            FilterMode.BLACKLIST -> !settings.filteredWorlds.contains(lower)
        }
    }

    private fun isPlayerAllowedForQuestion(player: Player, question: Question): Boolean {
        if (!isWorldAllowed(player.world.name)) return false
        val bank = questionCatalog.getBank(question.bankId) ?: return true
        if (bank.worlds.isEmpty()) return true
        return player.world.name.lowercase() in bank.worlds
    }

    private fun canPlayerAttemptQuestion(playerId: UUID, question: Question): Boolean {
        val event = activeEvent
        if (event != null) {
            if (playerId in event.answeredThisQuestion || isEventAnswerExpired(event)) return false
            answeredQuestions[playerId]?.remove(question.fullId)
            answeredQuestions[playerId]?.remove(question.id)
        } else {
            if (playerId in answeredThisRound || playerId in roundWinners) return false
        }
        if (settings.oneTryPerQuestion) {
            val answeredSet = answeredQuestions[playerId]
            if (answeredSet != null && (question.fullId in answeredSet || question.id in answeredSet)) {
                return false
            }
        }
        return true
    }

    fun nextQuestion() {
        if (activeEvent != null) return

        val onlinePlayers = Bukkit.getOnlinePlayers()
        if (onlinePlayers.size < settings.pacing.minPlayers) return

        val validPlayers = onlinePlayers.filter { isWorldAllowed(it.world.name) }
        val previousQuestion = currentQuestion

        if (previousQuestion != null && !answeredCorrectlyThisRound.get()) {
            if (settings.jackpot.enabled && currentJackpotMultiplier < settings.jackpot.maxMultiplier) {
                currentJackpotMultiplier++
                val bonusMoney = (currentJackpotMultiplier - 1) * settings.jackpot.moneyIncrement
                val jackpotMsg = messageManager.plain("quiz.jackpot-accumulated", mapOf(
                    "multiplier" to currentJackpotMultiplier,
                    "jackpot_bonus" to bonusMoney.toLong()
                ))
                for (player in validPlayers) {
                    taskRunner.runEntity(player, Runnable {
                        player.sendMessage(jackpotMsg)
                    })
                }
            }

            val formatted = messageManager.prefixed(
                "quiz.unanswered",
                mapOf("correct_option" to previousQuestion.answers.joinToString(","))
            )
            for (player in validPlayers) {
                taskRunner.runEntity(player, Runnable {
                    player.sendMessage(formatted)
                })
            }
        }

        if (validPlayers.isEmpty()) return

        val difficulties = settings.difficulties.values
        val totalWeight = difficulties.sumOf { it.weight }
        var selectedDifficulty: String? = null

        if (totalWeight > 0.0) {
            val randomWeight = Random.nextDouble() * totalWeight
            var accumulatedWeight = 0.0
            for (difficulty in difficulties) {
                accumulatedWeight += difficulty.weight
                if (randomWeight <= accumulatedWeight) {
                    selectedDifficulty = difficulty.key
                    break
                }
            }
        }

        if (selectedDifficulty == null && difficulties.isNotEmpty()) {
            selectedDifficulty = difficulties.first().key
        }

        val activeWorld = validPlayers.random().world.name
        val question = questionCatalog.random(allowedWorld = activeWorld, difficulty = selectedDifficulty)
            ?: questionCatalog.random(difficulty = selectedDifficulty)
            ?: return

        currentQuestion = question
        answeredThisRound.clear()
        roundWinners.clear()
        answeredCorrectlyThisRound.set(false)
        val generation = broadcastGeneration.incrementAndGet()

        antiBotService.onNewQuestionRound(question.fullId, question.options.keys)
        Bukkit.getPluginManager().callEvent(QuizQuestionBroadcastEvent(question, false))

        val targetPlayers = validPlayers.filter { isPlayerAllowedForQuestion(it, question) }
        if (targetPlayers.isEmpty()) return

        if (settings.useBossBar) {
            VisualUtil.showBossBar(
                taskRunner,
                settings.bossBarTitle.replace("{question}", question.text),
                settings.bossBarColor,
                settings.bossBarStyle,
                settings.quizInterval * 20L,
                targetPlayers,
                settings.broadcastBatchSize,
                settings.broadcastBatchDelayTicks
            )
        }

        broadcastQuestion(targetPlayers, generation)
    }

    fun checkScheduledEvents() {
        val now = LocalDateTime.now()
        val running = activeEvent
        if (running != null) {
            if (!isInsideEventWindow(running.config, now)) {
                finishEvent(running)
            }
            return
        }

        val event = settings.events.values.firstOrNull {
            it.enabled && isInsideEventWindow(it, now) && eventKey(it.id, now.toLocalDate()) !in startedEvents
        } ?: return
        startEvent(event.id, manual = false)
    }

    @Synchronized
    fun startEvent(id: String, manual: Boolean): String {
        if (activeEvent != null) return messageManager.prefixed("event.already-running")
        val event = settings.events[id] ?: return messageManager.prefixed("event.not-found", mapOf("id" to id))
        if (!event.enabled && !manual) return messageManager.prefixed("event.not-enabled", mapOf("id" to id))
        val active = ActiveQuizEvent(event, LocalDate.now())
        activeEvent = active
        startedEvents.add(eventKey(event.id, active.startedDate))
        scheduleEventQuestion(active, 1L)
        return messageManager.prefixed("event.started", mapOf("name" to event.name))
    }

    @Synchronized
    fun stopEvent(): String {
        val event = activeEvent ?: return messageManager.prefixed("event.no-running")
        finishEvent(event)
        return messageManager.prefixed("event.stopped", mapOf("name" to event.config.name))
    }

    fun eventStatus(): String {
        val event = activeEvent ?: return messageManager.plain("event.no-running")
        return messageManager.plain("event.status-format", mapOf(
            "name" to event.config.name,
            "question" to event.questionNumber,
            "total" to event.config.questionCount,
            "players" to event.stats.size
        ))
    }

    fun eventIds(): List<String> = settings.events.keys.sorted()

    private fun broadcastQuestion(players: Collection<Player>, generation: Long) {
        val batchSize = settings.broadcastBatchSize
        val delayStep = settings.broadcastBatchDelayTicks
        players.chunked(batchSize).forEachIndexed { index, batch ->
            val delay = (index * delayStep).coerceAtLeast(1L)
            taskRunner.runGlobalLater(delay, Runnable {
                if (generation != broadcastGeneration.get()) return@Runnable
                for (player in batch) {
                    if (!player.isOnline) continue
                    taskRunner.runEntity(player, Runnable {
                        if (generation == broadcastGeneration.get()) {
                            sendCurrentQuestion(player)
                        }
                    })
                }
            })
        }
    }

    fun sendCurrentQuestion(player: Player) {
        val question = currentQuestion ?: return
        if (!isPlayerAllowedForQuestion(player, question)) return
        val playerId = player.uniqueId
        if (!canPlayerAttemptQuestion(playerId, question)) return

        val difficulty = settings.difficulties[question.difficulty] ?: settings.difficulties.values.first()
        if (settings.useTitle) {
            VisualUtil.sendTitle(
                player,
                settings.titleMain,
                settings.titleSub.replace("{question}", question.text),
                10, 70, 20
            )
        }

        val rawTemplate = question.messages["question"] ?: return
        val bank = questionCatalog.getBank(question.bankId)
        val bankName = bank?.name ?: question.bankId

        val variables = mapOf(
            "difficulty" to difficulty.label,
            "quiz_id" to question.id,
            "full_id" to question.fullId,
            "bank_id" to question.bankId,
            "bank_name" to bankName,
            "question" to question.text,
            "jackpot" to currentJackpotMultiplier.toString()
        )

        val formattedMessage = MessageUtil.format(rawTemplate, variables + eventVars(activeEvent))
        player.sendMessage(formattedMessage.replace("{player_name}", player.name))

        if (question.soundClue != null) {
            SoundUtil.play(player, question.soundClue)
        }

        if (question.type != QuizType.INPUT) {
            val buttonFormat = messageManager.raw("quiz.button-text")
            val hoverFormat = messageManager.raw("quiz.button-hover")

            for ((optionKey, optionValue) in question.options) {
                val buttonText = MessageUtil.format(buttonFormat, mapOf("option" to optionKey, "text" to optionValue))
                val buttonComponent = TextComponent(*TextComponent.fromLegacyText(buttonText))

                if (question.type == QuizType.SINGLE) {
                    val token = antiBotService.getTokenForOption(optionKey) ?: optionKey
                    val hoverText = MessageUtil.format(hoverFormat, mapOf("option" to optionKey))
                    buttonComponent.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/cyuquiz answer $token")
                    buttonComponent.hoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(hoverText))
                }
                player.spigot().sendMessage(buttonComponent)
            }
        }

        if (question.type == QuizType.MULTIPLE) {
            player.sendMessage(messageManager.plain("quiz.multiple-hint"))
        } else if (question.type == QuizType.INPUT) {
            player.sendMessage(messageManager.plain("quiz.input-hint"))
        }

        SoundUtil.play(player, difficulty.getSound("new-question"))
    }

    fun handleChatGuess(player: Player, message: String): Boolean {
        val question = currentQuestion ?: return false
        if (!isPlayerAllowedForQuestion(player, question)) return false
        val playerId = player.uniqueId
        if (!canPlayerAttemptQuestion(playerId, question)) return false

        when (question.type) {
            QuizType.SINGLE -> {
                val choice = message.uppercase()
                if (choice in question.options.keys) {
                    return answer(player, choice, isChat = true)
                }
                val matchedKey = question.options.entries.firstOrNull { it.value.equals(message, ignoreCase = true) }?.key
                if (matchedKey != null) {
                    return answer(player, matchedKey, isChat = true)
                }
            }
            QuizType.MULTIPLE -> {
                val choiceSet = message.uppercase().replace(",", " ").split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
                if (choiceSet.isNotEmpty() && choiceSet.all { it in question.options.keys }) {
                    return answer(player, choiceSet.joinToString(","), isChat = true)
                }
            }
            QuizType.INPUT -> {
                val isCorrect = question.answers.any { it.equals(message, ignoreCase = true) }
                if (isCorrect) {
                    return answer(player, message, isChat = true)
                }
            }
        }
        return false
    }

    private fun evaluateAnswerChoice(question: Question, resolvedChoice: String): Pair<Boolean, String>? {
        return when (question.type) {
            QuizType.SINGLE -> {
                val choice = resolvedChoice.uppercase()
                if (choice !in question.options.keys) return null
                (choice in question.answers) to choice
            }
            QuizType.MULTIPLE -> {
                val choiceSet = resolvedChoice.uppercase().replace(",", " ").split("\\s+".toRegex()).filter { it.isNotBlank() }.toSet()
                if (choiceSet.isEmpty() || !choiceSet.all { it in question.options.keys }) return null
                val isCorrect = choiceSet == question.answers.map { it.uppercase() }.toSet()
                isCorrect to choiceSet.joinToString(",")
            }
            QuizType.INPUT -> {
                val isCorrect = question.answers.any { it.equals(resolvedChoice, ignoreCase = true) }
                isCorrect to resolvedChoice
            }
        }
    }

    fun answer(player: Player, pick: String, isChat: Boolean = false): Boolean {
        val question = currentQuestion ?: return false
        if (!isPlayerAllowedForQuestion(player, question)) return false
        val playerId = player.uniqueId
        if (!canPlayerAttemptQuestion(playerId, question)) return false

        val resolvedChoice: String
        when (val botResult = antiBotService.verifyAnswer(player, pick, isChat)) {
            is AntiBotResult.Allowed -> {
                resolvedChoice = botResult.resolvedChoice
            }
            is AntiBotResult.TooFast -> {
                player.sendMessage(messageManager.prefixed("anti-bot.too-fast", mapOf(
                    "latency" to botResult.latency,
                    "min_reaction" to botResult.minReactionMs
                )))
                SoundUtil.play(player, settings.cannotAnswerSound)
                return true
            }
            is AntiBotResult.IpLimit -> {
                player.sendMessage(messageManager.prefixed("anti-bot.ip-limit", mapOf(
                    "max_ip" to botResult.maxPerIp
                )))
                SoundUtil.play(player, settings.cannotAnswerSound)
                return true
            }
            is AntiBotResult.PenaltyCooldown -> {
                player.sendMessage(messageManager.prefixed("anti-bot.penalty-cooldown", mapOf(
                    "seconds" to botResult.remainingSeconds
                )))
                SoundUtil.play(player, settings.cannotAnswerSound)
                return true
            }
            is AntiBotResult.PlaytimeLimit -> {
                player.sendMessage(messageManager.prefixed("anti-bot.playtime-limit", mapOf(
                    "min_time" to botResult.minMinutes
                )))
                SoundUtil.play(player, settings.cannotAnswerSound)
                return true
            }
            is AntiBotResult.InvalidToken -> {
                player.sendMessage(messageManager.prefixed("anti-bot.invalid-token"))
                SoundUtil.play(player, settings.cannotAnswerSound)
                return true
            }
            AntiBotResult.Disallowed -> {
                return false
            }
        }

        antiBotService.recordAnswerSubmitted(player)

        val evaluation = evaluateAnswerChoice(question, resolvedChoice) ?: return false
        val isCorrect = evaluation.first
        val formattedChoice = evaluation.second

        if (settings.oneTryPerQuestion) {
            val playerAnsweredSet = answeredQuestions.computeIfAbsent(playerId) { ConcurrentHashMap.newKeySet() }
            playerAnsweredSet.add(question.fullId)
            playerAnsweredSet.add(question.id)
        }

        val event = activeEvent
        val stats = statsService.getStats(playerId)
        val rank: Int

        if (isCorrect) {
            roundWinners.add(playerId)
            rank = roundWinners.size
            event?.answeredThisQuestion?.add(playerId)
            if (rank == 1) {
                answeredCorrectlyThisRound.set(true)
            }
            stats.incrementCorrect()
            antiBotService.recordCorrect(playerId)
            handleGlobalFeverProgress()
        } else {
            rank = 0
            if (settings.oneTryPerQuestion) {
                event?.answeredThisQuestion?.add(playerId) ?: answeredThisRound.add(playerId)
            }
            stats.incrementWrong()
            antiBotService.recordWrong(playerId)
        }

        updateEventStats(event, playerId, isCorrect)

        Bukkit.getPluginManager().callEvent(
            QuizPlayerAnswerEvent(player, question, formattedChoice, isCorrect, stats.currentStreak)
        )

        val difficulty = settings.difficulties[question.difficulty] ?: settings.difficulties.values.first()
        val bank = questionCatalog.getBank(question.bankId)
        val bankName = bank?.name ?: question.bankId

        val variables = mapOf(
            "player_name" to player.name,
            "quiz_id" to question.id,
            "full_id" to question.fullId,
            "bank_id" to question.bankId,
            "bank_name" to bankName,
            "option" to formattedChoice,
            "correct_option" to question.answers.joinToString(","),
            "streak" to stats.currentStreak.toString(),
            "rank" to rank.toString(),
            "jackpot" to currentJackpotMultiplier.toString()
        )

        val rawMessage = question.messages[if (isCorrect) "on-correct" else "on-wrong"] ?: ""
        val formattedText = MessageUtil.format(rawMessage, variables + eventVars(event))

        taskRunner.runEntity(player, Runnable {
            if (isCorrect && rank > 1 && settings.tieredRewards.enabled) {
                val ratio = (settings.tieredRewards.ratios[rank] ?: 0.3) * 100.0
                player.sendMessage(messageManager.plain("quiz.tiered-winner", mapOf(
                    "player_name" to player.name,
                    "rank" to rank,
                    "ratio" to ratio.toInt()
                )))
            } else {
                player.sendMessage(formattedText)
            }

            SoundUtil.play(player, difficulty.getSound(if (isCorrect) "answer-correct" else "answer-wrong"))

            if (isCorrect) {
                val baseMultiplier = if (settings.tieredRewards.enabled) {
                    settings.tieredRewards.ratios[rank] ?: 1.0
                } else {
                    1.0
                }

                val finalMultiplier = if (isFeverActive()) {
                    baseMultiplier * settings.fever.rewardMultiplier
                } else if (rank == 1 && currentJackpotMultiplier > 1) {
                    baseMultiplier * currentJackpotMultiplier
                } else {
                    baseMultiplier
                }

                if (rank == 1 && currentJackpotMultiplier > 1) {
                    val jackpotWonMsg = messageManager.plain("quiz.jackpot-won", mapOf(
                        "player_name" to player.name,
                        "multiplier" to currentJackpotMultiplier
                    ))
                    for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                        if (isWorldAllowed(onlinePlayer.world.name)) {
                            taskRunner.runEntity(onlinePlayer, Runnable {
                                onlinePlayer.sendMessage(jackpotWonMsg)
                            })
                        }
                    }
                    currentJackpotMultiplier = 1
                }

                rewardService.grantRewards(player, question.rewards, variables, finalMultiplier)
                settings.streakRewards[stats.currentStreak]?.let {
                    rewardService.grantRewards(player, it, variables, finalMultiplier)
                }

                if (event == null && (!settings.tieredRewards.enabled || rank >= settings.tieredRewards.maxRanks)) {
                    answeredThisRound.add(playerId)
                    VisualUtil.hideBossBar()
                }
            }
        })

        return true
    }

    private fun handleGlobalFeverProgress() {
        if (!settings.fever.enabled || isFeverActive()) return
        val count = globalCorrectCounter.incrementAndGet()
        if (count >= settings.fever.threshold) {
            feverEndTimeMillis = System.currentTimeMillis() + settings.fever.durationSeconds * 1000L
            globalCorrectCounter.set(0)

            for (onlinePlayer in Bukkit.getOnlinePlayers()) {
                taskRunner.runEntity(onlinePlayer, Runnable {
                    VisualUtil.sendTitle(
                        onlinePlayer,
                        messageManager.raw("fever.started-title"),
                        messageManager.raw("fever.started-subtitle"),
                        10, 70, 20
                    )
                    onlinePlayer.sendMessage(messageManager.plain("fever.started-message"))
                })
            }
        }
    }

    fun getCurrentOptions(): List<String> {
        return currentQuestion?.options?.keys?.toList() ?: emptyList()
    }

    private fun scheduleEventQuestion(event: ActiveQuizEvent, delayTicks: Long) {
        val generation = event.generation + 1
        event.generation = generation
        taskRunner.runGlobalLater(delayTicks.coerceAtLeast(1L), Runnable {
            if (activeEvent !== event || event.generation != generation) return@Runnable
            nextEventQuestion(event)
        })
    }

    private fun nextEventQuestion(event: ActiveQuizEvent) {
        if (!isInsideEventWindow(event.config, LocalDateTime.now()) || event.questionNumber >= event.config.questionCount) {
            finishEvent(event)
            return
        }
        val validPlayers = Bukkit.getOnlinePlayers().filter { isEventWorldAllowed(event.config, it.world.name) }
        if (validPlayers.isEmpty()) {
            scheduleEventQuestion(event, event.config.questionIntervalSeconds * 20L)
            return
        }

        val question = pickEventQuestion(event) ?: run {
            finishEvent(event)
            return
        }
        currentQuestion = question
        event.questionNumber += 1
        event.askedQuestions.add(question.fullId)
        event.askedQuestions.add(question.id)
        event.answeredThisQuestion.clear()
        event.answerDeadlineMillis = System.currentTimeMillis() + event.config.answerSeconds * 1000L
        answeredCorrectlyThisRound.set(false)

        antiBotService.onNewQuestionRound(question.fullId, question.options.keys)
        Bukkit.getPluginManager().callEvent(QuizQuestionBroadcastEvent(question, true))

        if (settings.useBossBar) {
            VisualUtil.showBossBar(
                taskRunner,
                settings.bossBarTitle.replace("{question}", question.text),
                settings.bossBarColor,
                settings.bossBarStyle,
                event.config.answerSeconds * 20L,
                validPlayers,
                settings.broadcastBatchSize,
                settings.broadcastBatchDelayTicks
            )
        }

        broadcastQuestion(validPlayers, broadcastGeneration.incrementAndGet())
        scheduleEventQuestion(event, event.config.questionIntervalSeconds * 20L)
    }

    private fun pickEventQuestion(event: ActiveQuizEvent): Question? {
        val configuredPool = event.config.difficultyCounts.flatMap { (difficulty, count) ->
            questionCatalog.poolForDifficulty(difficulty, event.config.bankIds)
                .filter { it.fullId !in event.askedQuestions && it.id !in event.askedQuestions }
                .shuffled()
                .take(count)
        }
        val pool = configuredPool.ifEmpty {
            val allCandidates = if (event.config.bankIds.isNotEmpty()) {
                event.config.bankIds.flatMap { questionCatalog.getBank(it)?.questions.orEmpty() }
            } else {
                questionCatalog.allQuestions()
            }
            allCandidates.filter { it.fullId !in event.askedQuestions && it.id !in event.askedQuestions }
        }.ifEmpty {
            questionCatalog.allQuestions()
        }
        return pool.randomOrNull()
    }

    private fun finishEvent(event: ActiveQuizEvent) {
        if (activeEvent !== event) return
        activeEvent = null
        currentQuestion = null
        broadcastGeneration.incrementAndGet()
        event.answeredThisQuestion.clear()
        VisualUtil.hideBossBar()
        val winners = event.stats.entries
            .sortedWith(compareByDescending<Map.Entry<UUID, EventPlayerStats>> { it.value.score }.thenByDescending { it.value.correct })
            .take(10)
        val lines = mutableListOf<String>()
        lines.add(messageManager.plain("event.ended-broadcast", mapOf("name" to event.config.name)))
        if (winners.isEmpty()) {
            lines.add(messageManager.plain("event.ended-no-winner"))
        } else {
            winners.forEachIndexed { index, entry ->
                val playerName = Bukkit.getPlayer(entry.key)?.name ?: Bukkit.getOfflinePlayer(entry.key).name ?: messageManager.raw("top.unknown-player")
                val stats = entry.value
                lines.add(messageManager.plain("event.winner-format", mapOf(
                    "rank" to (index + 1),
                    "player_name" to playerName,
                    "score" to stats.score,
                    "correct" to stats.correct
                )))
            }
        }

        val players = Bukkit.getOnlinePlayers().filter { isEventWorldAllowed(event.config, it.world.name) }
        for (player in players) {
            taskRunner.runEntity(player, Runnable {
                lines.forEach { player.sendMessage(it) }
            })
        }
        grantEventRewards(event, winners)
    }

    private fun grantEventRewards(event: ActiveQuizEvent, winners: List<Map.Entry<UUID, EventPlayerStats>>) {
        for ((playerId, stats) in event.stats) {
            val player = Bukkit.getPlayer(playerId) ?: continue
            if (stats.answered <= 0) continue
            rewardService.grantRewards(player, event.config.participationRewards, eventRewardVars(event, player, stats, null))
        }
        winners.forEachIndexed { index, entry ->
            val rank = index + 1
            val rewards = event.config.topRewards[rank].orEmpty()
            if (rewards.isEmpty()) return@forEachIndexed
            val playerId = entry.key
            val player = Bukkit.getPlayer(playerId) ?: return@forEachIndexed
            rewardService.grantRewards(player, rewards, eventRewardVars(event, player, entry.value, rank))
        }
    }

    private fun updateEventStats(event: ActiveQuizEvent?, playerId: UUID, correct: Boolean) {
        if (event == null) return
        val stats = event.stats.computeIfAbsent(playerId) { EventPlayerStats() }
        synchronized(stats) {
            stats.answered += 1
            if (correct) {
                stats.correct += 1
                stats.score += 1
                stats.streak += 1
            } else {
                stats.wrong += 1
                stats.streak = 0
            }
        }
    }

    private fun isEventWorldAllowed(event: QuizEventConfig, worldName: String): Boolean {
        return event.worlds.isEmpty() || worldName.lowercase() in event.worlds
    }

    private fun isEventAnswerExpired(event: ActiveQuizEvent): Boolean {
        return event.answerDeadlineMillis > 0L && System.currentTimeMillis() > event.answerDeadlineMillis
    }

    private fun eventVars(event: ActiveQuizEvent?): Map<String, String> {
        if (event == null) return emptyMap()
        return mapOf(
            "event_id" to event.config.id,
            "event_name" to event.config.name,
            "event_question" to event.questionNumber.toString(),
            "event_total" to event.config.questionCount.toString()
        )
    }

    private fun eventRewardVars(event: ActiveQuizEvent, player: Player, stats: EventPlayerStats, rank: Int?): Map<String, String> {
        return mapOf(
            "player_name" to player.name,
            "event_id" to event.config.id,
            "event_name" to event.config.name,
            "rank" to (rank?.toString() ?: ""),
            "score" to stats.score.toString(),
            "correct" to stats.correct.toString(),
            "wrong" to stats.wrong.toString()
        )
    }

    private fun eventKey(id: String, date: LocalDate): String = "$id:$date"

    private fun isInsideEventWindow(event: QuizEventConfig, now: LocalDateTime): Boolean {
        val start = parseTime(event.startTime) ?: return false
        val end = parseTime(event.endTime) ?: return false
        val current = now.toLocalTime()
        return if (end.isAfter(start)) {
            !current.isBefore(start) && current.isBefore(end)
        } else {
            !current.isBefore(start) || current.isBefore(end)
        }
    }

    private fun parseTime(value: String): LocalTime? {
        return runCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
    }
}
