package org.cyuCBMclean.cyuquizReload.service

import org.bukkit.entity.Player
import org.cyuCBMclean.cyuquizReload.model.PlayerStats
import org.cyuCBMclean.cyuquizReload.storage.StatsStorage
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class StatsService(
    private val statsStorage: StatsStorage,
    private val useMySQL: Boolean
) {
    private val playerStats = ConcurrentHashMap<UUID, PlayerStats>()

    @Volatile
    private var cachedTop: List<Pair<UUID, Int>> = emptyList()
    private val lastTopUpdate = AtomicLong(0L)

    fun load() {
        statsStorage.init()
        if (useMySQL) {
            statsStorage.getTopCorrect(10).thenAccept { fetched ->
                cachedTop = fetched
                lastTopUpdate.set(System.currentTimeMillis())
            }
        } else {
            playerStats.clear()
            playerStats.putAll(statsStorage.loadAllLocal())
        }
    }

    fun save() {
        if (useMySQL) {
            for ((uuid, stats) in playerStats) {
                statsStorage.savePlayerSync(uuid, stats)
            }
        } else {
            statsStorage.saveAllLocal(playerStats)
        }
    }

    fun loadPlayer(player: Player) {
        if (!useMySQL) return
        statsStorage.loadPlayer(player.uniqueId).thenAccept { stats ->
            if (stats != null) {
                playerStats[player.uniqueId] = stats
            }
        }
    }

    fun saveAndRemovePlayer(player: Player) {
        if (!useMySQL) return
        val stats = playerStats.remove(player.uniqueId) ?: return
        statsStorage.savePlayer(player.uniqueId, stats)
    }

    fun getStats(uuid: UUID): PlayerStats {
        return playerStats.getOrPut(uuid) { PlayerStats() }
    }

    fun getTop(limit: Int): List<Pair<UUID, Int>> {
        if (useMySQL) {
            val now = System.currentTimeMillis()
            if (now - lastTopUpdate.get() > 30000L || cachedTop.isEmpty()) {
                statsStorage.getTopCorrect(limit.coerceAtLeast(10)).thenAccept { fetched ->
                    cachedTop = fetched
                    lastTopUpdate.set(System.currentTimeMillis())
                }
            }
            return if (cachedTop.isNotEmpty()) cachedTop.take(limit) else emptyList()
        }
        return playerStats.entries
            .sortedByDescending { it.value.correct }
            .take(limit)
            .map { it.key to it.value.correct }
    }
}