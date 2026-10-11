package org.cyuCBMclean.cyuquizReload.api

import org.cyuCBMclean.cyuquizReload.model.PlayerStats
import org.cyuCBMclean.cyuquizReload.model.Question
import org.cyuCBMclean.cyuquizReload.model.QuizBank
import java.util.UUID

interface CyuQuizApi {
    fun getCurrentQuestion(): Question?
    fun getPlayerStats(uuid: UUID): PlayerStats
    fun getTop(limit: Int): List<Pair<UUID, Int>>
    fun isEventActive(): Boolean
    fun getActiveEventName(): String?
    fun getBanks(): Collection<QuizBank>
    fun triggerNextQuestion()
}
