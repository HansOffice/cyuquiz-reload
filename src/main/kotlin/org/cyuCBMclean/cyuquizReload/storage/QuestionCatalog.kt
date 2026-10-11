package org.cyuCBMclean.cyuquizReload.storage

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.cyuCBMclean.cyuquizReload.model.Question
import org.cyuCBMclean.cyuquizReload.model.QuizBank
import org.cyuCBMclean.cyuquizReload.util.ResourceFiles
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class QuestionCatalog(private val plugin: JavaPlugin) {
    private val banks = ConcurrentHashMap<String, QuizBank>()
    private val questionsByFullId = ConcurrentHashMap<String, Question>()
    private val uniqueQuestions = CopyOnWriteArrayList<Question>()

    fun load(availableDifficulties: Set<String>) {
        migrateLegacyFileIfNeeded()

        ResourceFiles.ensure(plugin, "quizzes/minecraft.yml")
        ResourceFiles.ensure(plugin, "quizzes/general.yml")

        val quizzesDir = File(plugin.dataFolder, "quizzes")
        if (!quizzesDir.exists()) {
            quizzesDir.mkdirs()
        }

        val files = quizzesDir.listFiles { _, name -> name.endsWith(".yml", ignoreCase = true) }.orEmpty()
        require(files.isNotEmpty()) { "No quiz bank files found in quizzes/ directory" }

        val newBanks = mutableMapOf<String, QuizBank>()
        val newQuestions = mutableMapOf<String, Question>()

        for (file in files) {
            val bankId = file.nameWithoutExtension.lowercase()
            val config = YamlConfiguration.loadConfiguration(file)
            val name = config.getString("name", bankId) ?: bankId
            val enabled = config.getBoolean("enabled", true)
            val weight = config.getDouble("weight", 10.0).coerceAtLeast(0.0)
            val worlds = config.getStringList("worlds").map { it.lowercase() }.toSet()

            val rawQuestions = config.getMapList("questions")
            val bankQuestions = mutableListOf<Question>()

            for (questionMap in rawQuestions) {
                val question = Question.fromMap(questionMap, bankId)
                question.validate(availableDifficulties)
                bankQuestions.add(question)
                newQuestions[question.fullId] = question
                newQuestions.putIfAbsent(question.id, question)
            }

            newBanks[bankId] = QuizBank(
                id = bankId,
                name = name,
                enabled = enabled,
                weight = weight,
                worlds = worlds,
                questions = bankQuestions
            )
        }

        require(newQuestions.isNotEmpty()) { "No valid questions found across all banks in quizzes/" }

        val uniqueList = newBanks.values.flatMap { it.questions }

        banks.clear()
        banks.putAll(newBanks)

        questionsByFullId.clear()
        questionsByFullId.putAll(newQuestions)

        uniqueQuestions.clear()
        uniqueQuestions.addAll(uniqueList)
    }

    private fun migrateLegacyFileIfNeeded() {
        val legacyFile = File(plugin.dataFolder, "quizzes.yml")
        if (!legacyFile.exists()) return

        val quizzesDir = File(plugin.dataFolder, "quizzes")
        if (!quizzesDir.exists()) quizzesDir.mkdirs()

        val existingBanks = quizzesDir.listFiles { _, name -> name.endsWith(".yml", ignoreCase = true) }.orEmpty()
        if (existingBanks.isEmpty()) {
            val target = File(quizzesDir, "legacy.yml")
            legacyFile.copyTo(target, overwrite = false)
            plugin.logger.info("检测到旧版题库文件 quizzes.yml，已平滑迁移至 quizzes/legacy.yml")
        }
    }

    fun allBanks(): Collection<QuizBank> = banks.values

    fun getBank(id: String): QuizBank? = banks[id.lowercase()]

    fun countBanks(): Int = banks.size

    fun countQuestions(): Int = uniqueQuestions.size

    fun allQuestions(): List<Question> = uniqueQuestions

    fun getQuestion(id: String): Question? = questionsByFullId[id]

    fun poolForDifficulty(difficulty: String, bankIds: Set<String> = emptySet()): List<Question> {
        val candidateBanks = if (bankIds.isNotEmpty()) {
            banks.values.filter { it.enabled && it.id in bankIds }
        } else {
            banks.values.filter { it.enabled }
        }
        return candidateBanks.flatMap { it.questions }.filter { it.difficulty == difficulty }
    }

    fun random(
        allowedWorld: String? = null,
        difficulty: String? = null,
        bankIds: Set<String> = emptySet(),
        excludedIds: Set<String> = emptySet()
    ): Question? {
        val worldLower = allowedWorld?.lowercase()
        val candidateBanks = banks.values.filter { bank ->
            bank.enabled &&
            (bankIds.isEmpty() || bank.id in bankIds) &&
            (worldLower == null || bank.worlds.isEmpty() || worldLower in bank.worlds)
        }
        if (candidateBanks.isEmpty()) return null

        val totalWeight = candidateBanks.sumOf { it.weight }
        if (totalWeight > 0.0) {
            val randomRoll = Random.nextDouble() * totalWeight
            var accumulatedWeight = 0.0
            for (bank in candidateBanks) {
                accumulatedWeight += bank.weight
                if (randomRoll <= accumulatedWeight) {
                    val matchingInBank = bank.questions.filter {
                        (difficulty == null || it.difficulty == difficulty) &&
                        it.fullId !in excludedIds &&
                        it.id !in excludedIds
                    }
                    if (matchingInBank.isNotEmpty()) {
                        return matchingInBank[Random.nextInt(matchingInBank.size)]
                    }
                    break
                }
            }
        }

        val fallbackPool = candidateBanks.flatMap { it.questions }.filter {
            (difficulty == null || it.difficulty == difficulty) &&
            it.fullId !in excludedIds &&
            it.id !in excludedIds
        }
        if (fallbackPool.isNotEmpty()) {
            return fallbackPool[Random.nextInt(fallbackPool.size)]
        }

        val allPool = candidateBanks.flatMap { it.questions }
        if (allPool.isEmpty()) return null
        return allPool[Random.nextInt(allPool.size)]
    }
}
