package org.cyuCBMclean.cyuquizReload.util

import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.RemoteConsoleCommandSender
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Matcher
import java.util.regex.Pattern

object Text {
    private val hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})")
    private val miniHexPattern = Pattern.compile("<#([A-Fa-f0-9]{6})>")
    private val closeTagPattern = Pattern.compile("</#[A-Fa-f0-9]{6}>")
    private val gradientPattern = Pattern.compile(
        "<gradient:([#A-Za-z0-9_:]+)>(.*?)</gradient>",
        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
    )
    private val gradientPresets = mapOf(
        "fire" to listOf("FF416C", "FF4B2B"),
        "ice" to listOf("4FACFE", "00F2FE"),
        "aqua" to listOf("4FACFE", "00F2FE"),
        "ocean" to listOf("00C6FF", "0072FF"),
        "sky" to listOf("00C6FF", "0072FF"),
        "sunset" to listOf("F12711", "F5AF19"),
        "gold" to listOf("F5AF19", "E1A100", "FFEAA7"),
        "purple" to listOf("A18CD1", "FBC2EB"),
        "emerald" to listOf("11998E", "38EF7D"),
        "rainbow" to listOf("FF0000", "FFA500", "FFFF00", "00FF00", "00FFFF", "0000FF", "8B00FF")
    )
    private val tagMap = mapOf(
        "black" to "&0",
        "dark_blue" to "&1",
        "dark_green" to "&2",
        "dark_aqua" to "&3",
        "dark_red" to "&4",
        "dark_purple" to "&5",
        "gold" to "&6",
        "gray" to "&7",
        "dark_gray" to "&8",
        "blue" to "&9",
        "green" to "&a",
        "aqua" to "&b",
        "red" to "&c",
        "light_purple" to "&d",
        "yellow" to "&e",
        "white" to "&f",
        "bold" to "&l",
        "italic" to "&o",
        "underlined" to "&n",
        "strikethrough" to "&m",
        "reset" to "&r"
    )
    private val hexCache = ConcurrentHashMap<String, String>()

    fun color(raw: String?): String {
        if (raw.isNullOrEmpty()) return ""
        if (!raw.contains('<') && !raw.contains('#')) {
            return ChatColor.translateAlternateColorCodes('&', raw)
        }
        var value: String = raw
        if (value.contains('<')) {
            tagMap.forEach { (tag, code) ->
                value = value.replace("<$tag>", code, ignoreCase = true).replace("</$tag>", "", ignoreCase = true)
            }
            value = applyGradients(value)
            value = closeTagPattern.matcher(value).replaceAll("")
            value = miniHexPattern.matcher(value).replaceAll("&#$1")
        }
        if (value.contains('#')) {
            value = replaceHex(value)
        }
        return ChatColor.translateAlternateColorCodes('&', value)
    }

    fun send(sender: CommandSender, message: String) {
        sender.sendMessage(color(message))
    }

    fun sendMirrored(sender: CommandSender, message: String) {
        send(sender, message)
        if (sender is RemoteConsoleCommandSender && !Bukkit.isPrimaryThread()) {
            send(Bukkit.getConsoleSender(), message)
        }
    }

    private fun resolveGradientStops(rawSpec: String): List<String> {
        val lower = rawSpec.lowercase(Locale.ROOT).trim()
        gradientPresets[lower]?.let { return it }
        val parts = rawSpec.split(':').map { it.trim().removePrefix("#") }.filter { it.length == 6 }
        return if (parts.size >= 2) parts else listOf("4FACFE", "00F2FE")
    }

    private fun applyGradients(text: String): String {
        val matcher = gradientPattern.matcher(text)
        val buffer = StringBuffer()
        while (matcher.find()) {
            val spec = matcher.group(1)
            val inner = matcher.group(2)
            val bold = inner.contains("<bold>", ignoreCase = true) || inner.contains("&l") || inner.contains("§l")
            val plain = inner
                .replace(Regex("(?i)</?bold>"), "")
                .replace("&l", "")
                .replace("§l", "")
            val stops = resolveGradientStops(spec)
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(renderMultiGradient(plain, stops, bold)))
        }
        matcher.appendTail(buffer)
        return buffer.toString()
    }

    private fun renderMultiGradient(text: String, stops: List<String>, bold: Boolean): String {
        if (text.isEmpty()) return ""
        val parsedStops = stops.map { hex ->
            val num = hex.toIntOrNull(16) ?: 0xFFFFFF
            val r = num shr 16 and 0xFF
            val g = num shr 8 and 0xFF
            val b = num and 0xFF
            Triple(r, g, b)
        }
        if (parsedStops.size == 1) {
            val (r, g, b) = parsedStops[0]
            val hexStr = "&#%02X%02X%02X".format(r, g, b) + (if (bold) "&l" else "")
            return text.map { "$hexStr$it" }.joinToString("")
        }
        val chars = text.toCharArray()
        val segments = parsedStops.size - 1
        return buildString {
            for (index in chars.indices) {
                val progress = if (chars.size == 1) 0.0 else index.toDouble() / (chars.size - 1)
                val globalT = progress * segments
                val segIndex = globalT.toInt().coerceIn(0, segments - 1)
                val segT = (globalT - segIndex).coerceIn(0.0, 1.0)
                val (startRed, startGreen, startBlue) = parsedStops[segIndex]
                val (endRed, endGreen, endBlue) = parsedStops[segIndex + 1]
                val red = (startRed + (endRed - startRed) * segT).toInt().coerceIn(0, 255)
                val green = (startGreen + (endGreen - startGreen) * segT).toInt().coerceIn(0, 255)
                val blue = (startBlue + (endBlue - startBlue) * segT).toInt().coerceIn(0, 255)
                append("&#")
                append("%02X%02X%02X".format(red, green, blue))
                if (bold) append("&l")
                append(chars[index])
            }
        }
    }

    private fun replaceHex(text: String): String {
        val matcher = hexPattern.matcher(text)
        val buffer = StringBuffer()
        while (matcher.find()) {
            val hex = matcher.group(1).uppercase(Locale.ROOT)
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(hexCache.computeIfAbsent(hex, ::hexColor)))
        }
        matcher.appendTail(buffer)
        return buffer.toString()
    }

    private fun hexColor(hex: String): String {
        val normalized = hex.trimStart('#')
        if (normalized.length == 6) {
            return buildString(14) {
                append("§x")
                for (i in 0 until 6) {
                    append('§')
                    append(normalized[i].lowercaseChar())
                }
            }
        }
        return nearestLegacy(normalized)
    }

    private fun nearestLegacy(hex: String): String {
        val r = hex.substring(0, 2).toIntOrNull(16) ?: return "&f"
        val g = hex.substring(2, 4).toIntOrNull(16) ?: return "&f"
        val b = hex.substring(4, 6).toIntOrNull(16) ?: return "&f"
        val colors = listOf(
            '0' to Triple(0, 0, 0), '1' to Triple(0, 0, 170), '2' to Triple(0, 170, 0),
            '3' to Triple(0, 170, 170), '4' to Triple(170, 0, 0), '5' to Triple(170, 0, 170),
            '6' to Triple(255, 170, 0), '7' to Triple(170, 170, 170), '8' to Triple(85, 85, 85),
            '9' to Triple(85, 85, 255), 'a' to Triple(85, 255, 85), 'b' to Triple(85, 255, 255),
            'c' to Triple(255, 85, 85), 'd' to Triple(255, 85, 255), 'e' to Triple(255, 255, 85),
            'f' to Triple(255, 255, 255)
        )
        val best = colors.minByOrNull { (_, rgb) ->
            val dr = r - rgb.first
            val dg = g - rgb.second
            val db = b - rgb.third
            dr * dr + dg * dg + db * db
        }?.first ?: 'f'
        return "&$best"
    }
}
