package org.cyuCBMclean.cyuquizReload.util

import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.Player

object SoundUtil {
    fun playGlobal(sound: Sound?) {
        if (sound == null) return
        for (player in Bukkit.getOnlinePlayers()) {
            player.playSound(player.location, sound, 1f, 1f)
        }
    }

    fun play(player: Player, sound: Sound?) {
        if (sound == null) return
        player.playSound(player.location, sound, 1f, 1f)
    }
}