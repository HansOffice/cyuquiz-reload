package org.cyuCBMclean.cyuquizReload.model

data class QuizBank(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val weight: Double,
    val worlds: Set<String>,
    val questions: List<Question>
)
