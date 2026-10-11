package org.cyuCBMclean.cyuquizReload

import org.bukkit.Bukkit
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import org.cyuCBMclean.cyuquizReload.api.CyuQuizApi
import org.cyuCBMclean.cyuquizReload.api.DefaultCyuQuizApi
import org.cyuCBMclean.cyuquizReload.bridge.papi.CyuQuizPlaceholderExpansion
import org.cyuCBMclean.cyuquizReload.command.CyuquizCommand
import org.cyuCBMclean.cyuquizReload.config.ConfigUpgradeManager
import org.cyuCBMclean.cyuquizReload.config.MessageManager
import org.cyuCBMclean.cyuquizReload.config.Settings
import org.cyuCBMclean.cyuquizReload.listener.ChatListener
import org.cyuCBMclean.cyuquizReload.listener.JoinListener
import org.cyuCBMclean.cyuquizReload.listener.QuitListener
import org.cyuCBMclean.cyuquizReload.platform.CancellableTask
import org.cyuCBMclean.cyuquizReload.platform.PlatformHooks
import org.cyuCBMclean.cyuquizReload.platform.PlatformTaskRunner
import org.cyuCBMclean.cyuquizReload.service.AntiBotService
import org.cyuCBMclean.cyuquizReload.service.QuizService
import org.cyuCBMclean.cyuquizReload.service.RewardService
import org.cyuCBMclean.cyuquizReload.service.StatsService
import org.cyuCBMclean.cyuquizReload.storage.QuestionCatalog
import org.cyuCBMclean.cyuquizReload.storage.StatsStorage
import org.cyuCBMclean.cyuquizReload.util.VisualUtil
import java.io.File

class CyuquizReload : JavaPlugin() {

    lateinit var taskRunner: PlatformTaskRunner
        private set

    private lateinit var messageManager: MessageManager
    private lateinit var settings: Settings
    private lateinit var questionCatalog: QuestionCatalog
    private lateinit var statsStorage: StatsStorage
    private lateinit var statsService: StatsService
    private lateinit var antiBotService: AntiBotService
    private lateinit var rewardService: RewardService
    private lateinit var quizService: QuizService
    private lateinit var mainCommand: CyuquizCommand

    private var nextQuestionTask: CancellableTask? = null
    private var eventCheckTask: CancellableTask? = null
    private var papiExpansion: CyuQuizPlaceholderExpansion? = null
    private var freshInstall = false

    override fun onEnable() {
        freshInstall = !File(dataFolder, "config.yml").exists()
        ConfigUpgradeManager.prepare(this)
        saveDefaultConfig()

        taskRunner = PlatformTaskRunner(this)
        messageManager = MessageManager(this)
        messageManager.load()

        settings = Settings.load(this)

        questionCatalog = QuestionCatalog(this)
        questionCatalog.load(settings.difficulties.keys)

        statsStorage = StatsStorage(this, settings)
        statsService = StatsService(statsStorage, settings.useMySQL)
        statsService.load()

        antiBotService = AntiBotService(settings.antiBot)
        rewardService = RewardService(this, taskRunner)
        quizService = QuizService(
            this,
            settings,
            messageManager,
            questionCatalog,
            statsService,
            rewardService,
            antiBotService,
            taskRunner
        )

        server.servicesManager.register(
            CyuQuizApi::class.java,
            DefaultCyuQuizApi(quizService, statsService, questionCatalog),
            this,
            ServicePriority.Normal
        )

        mainCommand = CyuquizCommand(this, settings, messageManager, questionCatalog, quizService, statsService)
        getCommand("cyuquiz")?.setExecutor(mainCommand)
        getCommand("cyuquiz")?.tabCompleter = mainCommand

        val pm = server.pluginManager
        pm.registerEvents(ChatListener(quizService), this)
        pm.registerEvents(JoinListener(quizService, statsService), this)
        pm.registerEvents(QuitListener(statsService, antiBotService), this)

        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            val expansion = CyuQuizPlaceholderExpansion(this, statsService, quizService) { settings }
            if (expansion.register()) {
                papiExpansion = expansion
            }
        }

        quizService.nextQuestion()
        scheduleNextQuestion()
        scheduleEventCheck()

        logStartup()
    }

    override fun onDisable() {
        papiExpansion?.unregister()
        papiExpansion = null
        server.servicesManager.unregisterAll(this)

        cancelTasks()
        VisualUtil.hideBossBar()

        if (::statsService.isInitialized) {
            for (player in Bukkit.getOnlinePlayers()) {
                statsService.saveAndRemovePlayer(player)
            }
            statsService.save()
        }

        if (::statsStorage.isInitialized) {
            statsStorage.close()
        }

        val console = server.consoleSender
        console.sendMessage("")
        console.sendMessage("§8--------------------------------------------------")
        console.sendMessage("§b CyuQuiz-Reload §f- 问答系统与定时活动")
        console.sendMessage("§f")
        console.sendMessage("§7 ▸ §f状态 §7已保存并卸载")
        console.sendMessage("§8--------------------------------------------------")
        console.sendMessage("")
    }

    fun reloadPlugin() {
        reloadConfig()
        messageManager.load()
        settings = Settings.load(this)
        questionCatalog.load(settings.difficulties.keys)
        quizService.updateSettings(settings)
        mainCommand.updateSettings(settings)

        quizService.nextQuestion()
        scheduleNextQuestion()
        scheduleEventCheck()
    }

    private fun scheduleNextQuestion() {
        cancelNextQuestionTask()
        val interval = settings.quizInterval
        if (interval > 0) {
            val ticks = interval * 20L
            nextQuestionTask = taskRunner.runGlobalTimer(ticks, ticks, Runnable {
                quizService.nextQuestion()
            })
        }
    }

    private fun scheduleEventCheck() {
        eventCheckTask?.cancel()
        eventCheckTask = taskRunner.runGlobalTimer(20L, 20L * 20L, Runnable {
            quizService.checkScheduledEvents()
        })
    }

    private fun cancelTasks() {
        cancelNextQuestionTask()
        eventCheckTask?.cancel()
        eventCheckTask = null
    }

    private fun cancelNextQuestionTask() {
        nextQuestionTask?.cancel()
        nextQuestionTask = null
    }

    private fun logStartup() {
        val console = server.consoleSender
        console.sendMessage("")
        console.sendMessage("§8--------------------------------------------------")
        console.sendMessage("§b CyuQuiz-Reload §f- 问答系统与定时活动")
        console.sendMessage("§f")
        console.sendMessage("§7 ▸ §f版本 §b${description.version} §8| §f平台 §b${PlatformHooks.bandName()}")
        console.sendMessage("§7 ▸ §f已加载 §b${questionCatalog.countBanks()} §f个题库，共 §b${questionCatalog.countQuestions()} §f道题目")
        console.sendMessage("§7 ▸ §f数据存储 §b${if (settings.useMySQL) "MySQL" else "YAML"} §8| §f防脚本作答 ${if (settings.antiBot.enabled) "§b已启用" else "§7未启用"}")
        console.sendMessage("§7 ▸ §f交流群 §b331910315")
        console.sendMessage("§f")
        console.sendMessage("§7 ▸ §f状态 §b启动完成")
        if (freshInstall) {
            console.sendMessage("§7 ▸ §f首次安装 §b配置已生成")
        }
        console.sendMessage("§8--------------------------------------------------")
        console.sendMessage("")
    }
}
