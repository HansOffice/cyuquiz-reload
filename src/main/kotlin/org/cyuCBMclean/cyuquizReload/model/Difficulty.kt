package org.cyuCBMclean.cyuquizReload.model

import org.bukkit.Sound
import org.bukkit.configuration.ConfigurationSection

data class Difficulty(
    val key: String,
    val label: String,
    val weight: Double,
    val sounds: Map<String, Sound>
) {
    companion object {
        fun fromConfig(key: String, section: ConfigurationSection): Difficulty {
            val label = section.getString("label", "[$key]") ?: "[$key]"
            val weight = section.getDouble("weight", 1.0)
            val soundsSection = section.getConfigurationSection("sounds")
                ?: throw IllegalArgumentException("Difficulty $key is missing the 'sounds' section")

            val soundsMap = mutableMapOf<String, Sound>()
            for (soundKey in soundsSection.getKeys(false)) {
                val soundName = soundsSection.getString(soundKey)
                if (!soundName.isNullOrBlank()) {
                    try {
                        soundsMap[soundKey] = Sound.valueOf(soundName.uppercase())
                    } catch (_: IllegalArgumentException) {
                        throw IllegalArgumentException("Unknown sound '$soundName' in difficulty '$key'")
                    }
                }
            }
            return Difficulty(key, label, weight, soundsMap)
        }
    }

    fun getSound(type: String): Sound? = sounds[type]
}