package org.cyuCBMclean.cyuquizReload.model

import org.bukkit.Sound

enum class QuizType {
    SINGLE, MULTIPLE, INPUT
}

data class Question(
    val id: String,
    val type: QuizType,
    val text: String,
    val options: Map<String, String>,
    val answers: Set<String>,
    val difficulty: String,
    val messages: Map<String, String>,
    val rewards: List<String>,
    val bankId: String = "",
    val soundClue: Sound? = null
) {
    val fullId: String = if (bankId.isNotBlank()) "$bankId:$id" else id

    companion object {
        fun fromMap(map: Map<*, *>, bankId: String = ""): Question {
            val id = map["id"].toString()
            val typeStr = map["type"]?.toString()?.uppercase() ?: "SINGLE"
            val type = try {
                QuizType.valueOf(typeStr)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("Unknown question type '$typeStr' in question '$id'")
            }
            val text = map["text"].toString()
            val difficulty = map["difficulty"].toString()

            val optionsMap = (map["options"] as? Map<*, *>)?.entries?.associate {
                it.key.toString() to it.value.toString()
            } ?: emptyMap()

            val answersList = (map["answer"] as? List<*>)?.map {
                if (type == QuizType.INPUT) it.toString() else it.toString().uppercase()
            }?.toSet() ?: emptySet()

            val messagesMap = (map["messages"] as? Map<*, *>)?.entries?.associate {
                it.key.toString() to it.value.toString()
            } ?: emptyMap()

            val rewardsList = (map["rewards"] as? List<*>)?.map { it.toString() } ?: emptyList()

            val soundClueStr = map["sound-clue"]?.toString()
            val soundClue = if (!soundClueStr.isNullOrBlank()) {
                try {
                    Sound.valueOf(soundClueStr.uppercase())
                } catch (_: IllegalArgumentException) {
                    throw IllegalArgumentException("Unknown sound-clue '$soundClueStr' in question '$id'")
                }
            } else null

            return Question(id, type, text, optionsMap, answersList, difficulty, messagesMap, rewardsList, bankId, soundClue)
        }
    }

    fun validate(validDifficulties: Set<String>) {
        require(id.isNotBlank() && text.isNotBlank())
        if (type != QuizType.INPUT) {
            require(options.size >= 2)
            require(answers.isNotEmpty() && answers.all { it.uppercase() in options.keys })
        } else {
            require(answers.isNotEmpty())
        }
        require(difficulty in validDifficulties)
        require(listOf("question", "on-correct", "on-wrong").all { it in messages.keys })
        require(rewards.isNotEmpty())
    }
}