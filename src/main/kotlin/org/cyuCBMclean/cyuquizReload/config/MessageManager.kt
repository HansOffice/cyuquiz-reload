package org.cyuCBMclean.cyuquizReload.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.cyuCBMclean.cyuquizReload.util.ResourceFiles
import org.cyuCBMclean.cyuquizReload.util.Text
import java.io.File
import java.util.Locale

class MessageManager(private val plugin: JavaPlugin) {
    private lateinit var file: File
    private lateinit var config: YamlConfiguration

    fun load() {
        ResourceFiles.ensure(plugin, "lang/zh_cn.yml")
        ResourceFiles.ensure(plugin, "lang/en_us.yml")
        val language = plugin.config.getString("language", "zh_cn")?.lowercase(Locale.ROOT) ?: "zh_cn"
        val path = "lang/$language.yml"
        file = ResourceFiles.ensure(plugin, if (plugin.getResource(path) != null) path else "lang/zh_cn.yml")
        config = YamlConfiguration.loadConfiguration(file)
    }

    fun raw(path: String): String = config.getString(path) ?: path

    fun raw(path: String, fallback: String): String = config.getString(path, fallback) ?: fallback

    fun prefixed(path: String, values: Map<String, Any?> = emptyMap()): String {
        val prefix = raw("prefix")
        return Text.color(prefix + replace(raw(path), values))
    }

    fun plain(path: String, values: Map<String, Any?> = emptyMap()): String {
        return Text.color(replace(raw(path), values))
    }

    private fun replace(text: String, values: Map<String, Any?>): String {
        var result = text
        for ((key, value) in values) {
            result = result.replace("{$key}", value?.toString() ?: "")
        }
        return result
    }
}
