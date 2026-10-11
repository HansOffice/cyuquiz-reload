package org.cyuCBMclean.cyuquizReload.util

import org.bukkit.plugin.java.JavaPlugin
import java.io.File

object ResourceFiles {
    fun ensure(plugin: JavaPlugin, path: String): File {
        val file = File(plugin.dataFolder, path)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            plugin.saveResource(path, false)
        }
        return file
    }
}
