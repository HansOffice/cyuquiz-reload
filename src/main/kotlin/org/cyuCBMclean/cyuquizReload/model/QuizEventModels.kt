package org.cyuCBMclean.cyuquizReload.model

data class QuizEventConfig(
    val id: String,
    val enabled: Boolean,
    val name: String,
    val worlds: Set<String>,
    val startTime: String,
    val endTime: String,
    val questionCount: Int,
    val questionIntervalSeconds: Long,
    val answerSeconds: Long,
    val difficultyCounts: Map<String, Int>,
    val participationRewards: List<String>,
    val topRewards: Map<Int, List<String>>,
    val bankIds: Set<String> = emptySet()
)

data class EventPlayerStats(
    var answered: Int = 0,
    var correct: Int = 0,
    var wrong: Int = 0,
    var score: Int = 0,
    var streak: Int = 0
)
