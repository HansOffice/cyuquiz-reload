package org.cyuCBMclean.cyuquizReload.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.cyuCBMclean.cyuquizReload.service.QuizService

class ChatListener(private val quizService: QuizService) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onChat(event: AsyncPlayerChatEvent) {
        if (quizService.handleChatGuess(event.player, event.message.trim())) {
            event.isCancelled = true
        }
    }
}