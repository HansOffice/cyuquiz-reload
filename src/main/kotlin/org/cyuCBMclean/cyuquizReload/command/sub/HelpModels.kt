package org.cyuCBMclean.cyuquizReload.command.sub

data class HelpLine(
    val command: String,
    val description: String,
    val permission: String?
)

val QUIZ_HELP_LINES = listOf(
    HelpLine("/cyuquiz help [页码]", "查看问答系统帮助", null),
    HelpLine("/cyuquiz answer <答案>", "回答当前问题", "cyuquiz.use"),
    HelpLine("/cyuquiz stats", "查看个人答题统计", "cyuquiz.use"),
    HelpLine("/cyuquiz top", "查看答题正确数排行榜", "cyuquiz.use"),
    HelpLine("/cyuquiz bank list", "查看所有已加载题库与题目数量", "cyuquiz.use"),
    HelpLine("/cyuquiz reload", "重载插件配置与题库", "cyuquiz.reload"),
    HelpLine("/cyuquiz event list", "查看所有已配置的活动", "cyuquiz.event.admin"),
    HelpLine("/cyuquiz event status", "查看当前进行中的活动状态", "cyuquiz.event.admin"),
    HelpLine("/cyuquiz event start <活动ID>", "手动开启指定答题活动", "cyuquiz.event.admin"),
    HelpLine("/cyuquiz event stop", "手动终止当前进行中的活动", "cyuquiz.event.admin")
)
