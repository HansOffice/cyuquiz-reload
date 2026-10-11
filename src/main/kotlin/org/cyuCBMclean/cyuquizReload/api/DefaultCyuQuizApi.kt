package org.cyuCBMclean.cyuquizReload.api

import org.cyuCBMclean.cyuquizReload.model.PlayerStats
import org.cyuCBMclean.cyuquizReload.model.Question
import org.cyuCBMclean.cyuquizReload.model.QuizBank
import org.cyuCBMclean.cyuquizReload.service.QuizService
import org.cyuCBMclean.cyuquizReload.service.StatsService
import org.cyuCBMclean.cyuquizReload.storage.QuestionCatalog
import java.util.UUID

class DefaultCyuQuizApi(
    private val quizService: QuizService,
    private val statsService: StatsService,
    private val questionCatalog: QuestionCatalog
) : CyuQuizApi {

    override fun getCurrentQuestion(): Question? = quizService.currentQuestion

    override fun getPlayerStats(uuid: UUID): PlayerStats = statsService.getStats(uuid)

    override fun getTop(limit: Int): List<Pair<UUID, Int>> = statsService.getTop(limit)

    override fun isEventActive(): Boolean = quizService.isEventRunning()

    override fun getActiveEventName(): String? = quizService.runningEventName()

    override fun getBanks(): Collection<QuizBank> = questionCatalog.allBanks()

    override fun triggerNextQuestion() {
        quizService.nextQuestion()
    }
}
