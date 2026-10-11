package org.cyuCBMclean.cyuquizReload.api

import org.bukkit.Bukkit

object CyuQuizProvider {
    val api: CyuQuizApi
        get() = Bukkit.getServicesManager().load(CyuQuizApi::class.java)
            ?: throw IllegalStateException("CyuQuizApi 服务未注册，请确保 CyuQuiz-Reload 插件已启用")
}
