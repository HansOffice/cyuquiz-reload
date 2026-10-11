package org.cyuCBMclean.cyuquizReload.command.sub

import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.cyuCBMclean.cyuquizReload.util.Text

class HelpRenderer(private val pageSize: Int = 6) {

    fun availableHelp(sender: CommandSender, lines: List<HelpLine>): List<HelpLine> =
        lines.filter { it.permission == null || sender.hasPermission(it.permission) }

    fun render(
        sender: CommandSender,
        lines: List<HelpLine>,
        rawPage: String?,
        pageCommand: String = "/cyuquiz help"
    ) {
        val entries = availableHelp(sender, lines)
        val size = pageSize.coerceAtLeast(1)
        val totalPages = maxOf(1, (entries.size + size - 1) / size)
        val parsed = rawPage?.toIntOrNull()

        if (rawPage != null && parsed == null) {
            Text.send(sender, "&#F87171× 页码无效，请输入数字")
            return
        }

        val page = if (parsed == null || parsed in 1..totalPages) {
            parsed ?: 1
        } else {
            Text.send(sender, "&#8A96A8帮助页码超出范围，已为你显示第 &#D7DEE81 &#8A96A8页，共 &#D7DEE8$totalPages &#8A96A8页")
            1
        }

        val start = (page - 1) * size
        val pageEntries = entries.subList(start, minOf(start + size, entries.size))

        Text.send(sender, "&#3A4352&m┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈")
        Text.send(sender, "&#D7DEE8&lCyuQuiz &#8A96A8系统帮助 &#8A96A8(第 &#D7DEE8$page&#8A96A8/&#D7DEE8$totalPages &#8A96A8页)")

        if (sender is Player) {
            for (entry in pageEntries) {
                val lineText = Text.color("&#D7DEE8› ${entry.command} &#8A96A8${entry.description}")
                val click = ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, entry.command)
                val hoverText = Text.color("&#8A96A8点击在聊天框填入指令 &#D7DEE8${entry.command}")
                val hover = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(hoverText)
                )
                val component = TextComponent(*TextComponent.fromLegacyText(lineText))
                component.clickEvent = click
                component.hoverEvent = hover
                sender.spigot().sendMessage(component)
            }
            if (totalPages > 1) {
                val prevPage = if (page > 1) page - 1 else totalPages
                val nextPage = if (page < totalPages) page + 1 else 1
                val prevHover = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(Text.color("&#8A96A8点击查看第 &#D7DEE8$prevPage &#8A96A8页"))
                )
                val nextHover = HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    TextComponent.fromLegacyText(Text.color("&#8A96A8点击查看第 &#D7DEE8$nextPage &#8A96A8页"))
                )

                val prevBtn = TextComponent(*TextComponent.fromLegacyText(Text.color("&#7DD3FC‹ 上一页")))
                prevBtn.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "$pageCommand $prevPage")
                prevBtn.hoverEvent = prevHover

                val nextBtn = TextComponent(*TextComponent.fromLegacyText(Text.color("&#7DD3FC下一页 ›")))
                nextBtn.clickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "$pageCommand $nextPage")
                nextBtn.hoverEvent = nextHover

                val footer = TextComponent("")
                footer.addExtra(prevBtn)
                footer.addExtra(TextComponent(*TextComponent.fromLegacyText(Text.color(" &#8A96A8| "))))
                footer.addExtra(nextBtn)
                sender.spigot().sendMessage(footer)
            }
        } else {
            for (entry in pageEntries) {
                Text.send(sender, "&#D7DEE8› ${entry.command} &#8A96A8${entry.description}")
            }
        }
        Text.send(sender, "&#3A4352&m┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈┈")
    }
}
