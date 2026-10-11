package org.cyuCBMclean.cyuquizReload.api.event

import org.bukkit.Bukkit
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.cyuCBMclean.cyuquizReload.model.Question

class QuizQuestionBroadcastEvent(
    val question: Question,
    val isEvent: Boolean,
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
