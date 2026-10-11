package org.cyuCBMclean.cyuquizReload.listener

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.cyuCBMclean.cyuquizReload.command.CyuquizCommand
import org.cyuCBMclean.cyuquizReload.service.AntiBotService
import org.cyuCBMclean.cyuquizReload.service.StatsService
import org.cyuCBMclean.cyuquizReload.util.VisualUtil

class QuitListener(
    private val statsService: StatsService,
    private val antiBotService: AntiBotService
) : Listener {

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        val player = event.player
        statsService.saveAndRemovePlayer(player)
        antiBotService.clearPlayer(player.uniqueId)
        CyuquizCommand.clearDebounce(player.uniqueId)
        VisualUtil.removePlayer(player)
    }
}