package org.cyuCBMclean.cyuquizReload.api.event

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.cyuCBMclean.cyuquizReload.model.Question

class QuizPlayerAnswerEvent(
    val player: Player,
    val question: Question,
    val answer: String,
    val isCorrect: Boolean,
    val currentStreak: Int,
    async: Boolean = !Bukkit.isPrimaryThread()
) : Event(async) {

    companion object {
        @JvmStatic
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = HANDLERS
    }

    override fun getHandlers(): HandlerList = HANDLERS
}
