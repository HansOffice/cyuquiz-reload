package org.cyuCBMclean.cyuquizReload.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.cyuCBMclean.cyuquizReload.service.QuizService
import org.cyuCBMclean.cyuquizReload.service.StatsService

class JoinListener(
    private val quizService: QuizService,
    private val statsService: StatsService
) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        statsService.loadPlayer(event.player)
        quizService.sendCurrentQuestion(event.player)
    }
}