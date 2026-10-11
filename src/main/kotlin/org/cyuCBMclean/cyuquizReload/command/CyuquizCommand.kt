package org.cyuCBMclean.cyuquizReload.command

import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.cyuCBMclean.cyuquizReload.CyuquizReload
import org.cyuCBMclean.cyuquizReload.command.sub.HelpRenderer
import org.cyuCBMclean.cyuquizReload.command.sub.QUIZ_HELP_LINES
import org.cyuCBMclean.cyuquizReload.config.MessageManager
import org.cyuCBMclean.cyuquizReload.config.Settings
import org.cyuCBMclean.cyuquizReload.service.QuizService
import org.cyuCBMclean.cyuquizReload.service.StatsService
import org.cyuCBMclean.cyuquizReload.storage.QuestionCatalog
import org.cyuCBMclean.cyuquizReload.util.SoundUtil
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class CyuquizCommand(
    private val plugin: CyuquizReload,
    private var settings: Settings,
    private val messageManager: MessageManager,
    private val questionCatalog: QuestionCatalog,
    private val quizService: QuizService,
    private val statsService: StatsService
) : CommandExecutor, TabCompleter {

    companion object {
        private val debounceMap = ConcurrentHashMap<UUID, Long>()

        fun clearDebounce(uuid: UUID) {
            debounceMap.remove(uuid)
        }
    }

    private val helpRenderer = HelpRenderer()

    fun updateSettings(newSettings: Settings) {
        settings = newSettings
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty() || args[0].equals("help", ignoreCase = true)) {
            val page = if (args.size > 1) args[1] else null
            helpRenderer.render(sender, QUIZ_HELP_LINES, page)
            return true
        }

        when (args[0].lowercase()) {
            "reload" -> handleReload(sender)
            "answer" -> handleAnswer(sender, args)
            "stats" -> handleStats(sender)
            "top" -> handleTop(sender)
            "bank" -> handleBank(sender, args)
            "event" -> handleEvent(sender, args)
            else -> helpRenderer.render(sender, QUIZ_HELP_LINES, null)
        }
        return true
    }

    private fun handleReload(sender: CommandSender) {
        if (!sender.hasPermission("cyuquiz.reload")) {
            sender.sendMessage(messageManager.prefixed("command.no-permission"))
            return
        }
        sender.sendMessage(messageManager.prefixed("command.reloading"))
        try {
            plugin.reloadPlugin()
            sender.sendMessage(messageManager.prefixed("command.reload-success"))
        } catch (exception: Exception) {
            sender.sendMessage(messageManager.prefixed("command.reload-failure", mapOf("error" to (exception.message ?: "Unknown Error"))))
        }
    }

    private fun handleAnswer(sender: CommandSender, args: Array<out String>) {
        if (sender !is Player) {
            sender.sendMessage(messageManager.prefixed("command.not-player"))
            return
        }
        if (!sender.hasPermission("cyuquiz.use")) {
            sender.sendMessage(messageManager.prefixed("command.no-permission"))
            return
        }

        val currentTime = System.currentTimeMillis()
        val lastClickTime = debounceMap[sender.uniqueId] ?: 0L
        if (currentTime - lastClickTime < 250L) return
        debounceMap[sender.uniqueId] = currentTime

        val chosenOption = if (args.size > 1) args[1] else ""
        if (!quizService.answer(sender, chosenOption, isChat = false)) {
            sender.sendMessage(messageManager.prefixed("quiz.cannot-answer"))
            SoundUtil.play(sender, settings.cannotAnswerSound)
        }
    }

    private fun handleStats(sender: CommandSender) {
        if (!sender.hasPermission("cyuquiz.use")) {
            sender.sendMessage(messageManager.prefixed("command.no-permission"))
            return
        }
        if (sender !is Player) {
            sender.sendMessage(messageManager.prefixed("command.not-player"))
            return
        }
        val stats = statsService.getStats(sender.uniqueId)
        if (stats.answered == 0) {
            sender.sendMessage(messageManager.prefixed("quiz.no-data"))
            return
        }
        sender.sendMessage(messageManager.prefixed("quiz.summary", mapOf(
            "answered" to stats.answered,
            "correct" to stats.correct,
            "wrong" to stats.wrong,
            "highest_streak" to stats.highestStreak
        )))
    }

    private fun handleTop(sender: CommandSender) {
        if (!sender.hasPermission("cyuquiz.use")) {
            sender.sendMessage(messageManager.prefixed("command.no-permission"))
            return
        }
        val topList = statsService.getTop(10)
        sender.sendMessage(messageManager.plain("top.header"))
        sender.sendMessage(messageManager.plain("top.title"))
        sender.sendMessage("")

        if (topList.isEmpty()) {
            sender.sendMessage(messageManager.plain("top.empty"))
        } else {
            val format = messageManager.raw("top.format")
            val unknown = messageManager.raw("top.unknown-player")
            topList.forEachIndexed { index, (playerId, correctCount) ->
                val name = Bukkit.getPlayer(playerId)?.name ?: Bukkit.getOfflinePlayer(playerId).name ?: unknown
                sender.sendMessage(messageManager.plain("top.format", mapOf(
                    "rank" to (index + 1),
                    "player_name" to name,
                    "correct" to correctCount
                )))
            }
        }
        sender.sendMessage(messageManager.plain("top.footer"))
    }

    private fun handleBank(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("cyuquiz.use")) {
            sender.sendMessage(messageManager.prefixed("command.no-permission"))
            return
        }
        val banks = questionCatalog.allBanks()
        if (banks.isEmpty()) {
            sender.sendMessage(messageManager.prefixed("command.bank-empty"))
            return
        }
        sender.sendMessage(messageManager.plain("command.bank-list-header"))
        sender.sendMessage(messageManager.plain("command.bank-list-title", mapOf("total" to banks.size)))
        sender.sendMessage("")
        for (bank in banks.sortedBy { it.id }) {
            val status = if (bank.enabled) messageManager.plain("command.bank-enabled") else messageManager.plain("command.bank-disabled")
            sender.sendMessage(messageManager.plain("command.bank-entry", mapOf(
                "bank_id" to bank.id,
                "name" to bank.name,
                "count" to bank.questions.size,
                "status" to status
            )))
        }
        sender.sendMessage(messageManager.plain("command.bank-list-header"))
    }

    private fun handleEvent(sender: CommandSender, args: Array<out String>) {
        if (!sender.hasPermission("cyuquiz.event.admin")) {
            sender.sendMessage(messageManager.prefixed("command.no-permission"))
            return
        }
        when (args.getOrNull(1)?.lowercase()) {
            "list" -> {
                val eventIds = quizService.eventIds()
                if (eventIds.isEmpty()) {
                    sender.sendMessage(messageManager.prefixed("command.event-no-configured"))
                } else {
                    sender.sendMessage(messageManager.prefixed("command.event-list", mapOf("events" to eventIds.joinToString(", "))))
                }
            }
            "status" -> sender.sendMessage(messageManager.prefixed("command.event-status", mapOf("status" to quizService.eventStatus())))
            "start" -> {
                val eventId = args.getOrNull(2)
                if (eventId.isNullOrBlank()) {
                    sender.sendMessage(messageManager.prefixed("command.event-usage"))
                    return
                }
                sender.sendMessage(quizService.startEvent(eventId, manual = true))
            }
            "stop" -> sender.sendMessage(quizService.stopEvent())
            else -> sender.sendMessage(messageManager.prefixed("command.event-usage"))
        }
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>): List<String> {
        if (args.size == 1) {
            val completions = mutableListOf<String>()
            if (sender.hasPermission("cyuquiz.use")) {
                completions.add("answer")
                completions.add("stats")
                completions.add("top")
                completions.add("bank")
                completions.add("help")
            }
            if (sender.hasPermission("cyuquiz.reload")) {
                completions.add("reload")
            }
            if (sender.hasPermission("cyuquiz.event.admin")) {
                completions.add("event")
            }
            return completions.filter { it.startsWith(args[0], ignoreCase = true) }
        }

        if (args.size == 2 && args[0].equals("bank", ignoreCase = true) && sender.hasPermission("cyuquiz.use")) {
            return listOf("list").filter { it.startsWith(args[1], ignoreCase = true) }
        }

        if (args.size == 2 && args[0].equals("answer", ignoreCase = true) && sender.hasPermission("cyuquiz.use")) {
            return quizService.getCurrentOptions().filter { it.startsWith(args[1], ignoreCase = true) }
        }
        if (args.size == 2 && args[0].equals("event", ignoreCase = true) && sender.hasPermission("cyuquiz.event.admin")) {
            return listOf("list", "status", "start", "stop").filter { it.startsWith(args[1], ignoreCase = true) }
        }
        if (args.size == 3 && args[0].equals("event", ignoreCase = true) && args[1].equals("start", ignoreCase = true) && sender.hasPermission("cyuquiz.event.admin")) {
            return quizService.eventIds().filter { it.startsWith(args[2], ignoreCase = true) }
        }

        return emptyList()
    }
}
