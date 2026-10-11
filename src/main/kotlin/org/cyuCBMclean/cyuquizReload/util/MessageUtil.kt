package org.cyuCBMclean.cyuquizReload.util

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object MessageUtil {
    private val isPapiEnabled: Boolean
        get() = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")

    fun color(text: String): String {
        return Text.color(text)
    }

    fun format(template: String, vars: Map<String, String>): String {
        if (vars.isEmpty() || !template.contains('{')) {
            return color(template)
        }
        var result = template
        for ((key, value) in vars) {
            result = result.replace("{$key}", value)
        }
        return color(result)
    }

    fun setPlaceholders(player: Player?, text: String): String {
        if (player == null || !isPapiEnabled) return text
        return PlaceholderAPI.setPlaceholders(player, text)
    }
}