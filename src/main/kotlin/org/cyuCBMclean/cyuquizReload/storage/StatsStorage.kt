package org.cyuCBMclean.cyuquizReload.storage

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import org.cyuCBMclean.cyuquizReload.config.Settings
import org.cyuCBMclean.cyuquizReload.model.PlayerStats
import java.io.File
import java.util.UUID
import java.util.concurrent.CompletableFuture

class StatsStorage(
    private val plugin: JavaPlugin,
    private val settings: Settings
) {
    private val statsFile = File(plugin.dataFolder, "stats.yml")
    private var dataSource: HikariDataSource? = null

    fun init() {
        if (!settings.useMySQL) {
            if (!statsFile.exists()) {
                plugin.dataFolder.mkdirs()
                statsFile.createNewFile()
            }
            return
        }
        val config = HikariConfig().apply {
            jdbcUrl = settings.mysqlUri
            maximumPoolSize = 10
            minimumIdle = 2
            connectionTimeout = 10000L
        }
        dataSource = HikariDataSource(config)
        setupTable()
    }

    fun close() {
        dataSource?.close()
        dataSource = null
    }

    private fun setupTable() {
        CompletableFuture.runAsync {
            try {
                dataSource?.connection?.use { connection ->
                    connection.prepareStatement(
                        "CREATE TABLE IF NOT EXISTS cyuquiz_stats (" +
                                "uuid VARCHAR(36) PRIMARY KEY," +
                                "answered INT NOT NULL DEFAULT 0," +
                                "correct INT NOT NULL DEFAULT 0," +
                                "wrong INT NOT NULL DEFAULT 0," +
                                "current_streak INT NOT NULL DEFAULT 0," +
                                "highest_streak INT NOT NULL DEFAULT 0" +
                                ")"
                    ).use { statement -> statement.execute() }
                }
            } catch (exception: Exception) {
                plugin.logger.severe("初始化 MySQL 数据表失败: ${exception.message}")
            }
        }
    }

    fun loadAllLocal(): Map<UUID, PlayerStats> {
        if (!statsFile.exists()) return emptyMap()
        val config = YamlConfiguration.loadConfiguration(statsFile)
        val section = config.getConfigurationSection("stats") ?: return emptyMap()
        val result = mutableMapOf<UUID, PlayerStats>()
        for (playerUuidKey in section.getKeys(false)) {
            val uuid = try {
                UUID.fromString(playerUuidKey)
            } catch (_: IllegalArgumentException) {
                continue
            }
            val answered = config.getInt("stats.$playerUuidKey.answered", 0)
            val correct = config.getInt("stats.$playerUuidKey.correct", 0)
            val wrong = config.getInt("stats.$playerUuidKey.wrong", 0)
            val currentStreak = config.getInt("stats.$playerUuidKey.currentStreak", 0)
            val highestStreak = config.getInt("stats.$playerUuidKey.highestStreak", 0)
            result[uuid] = PlayerStats(answered, correct, wrong, currentStreak, highestStreak)
        }
        return result
    }

    fun saveAllLocal(statsMap: Map<UUID, PlayerStats>) {
        val config = YamlConfiguration()
        for ((uuid, stats) in statsMap) {
            val key = uuid.toString()
            config.set("stats.$key.answered", stats.answered)
            config.set("stats.$key.correct", stats.correct)
            config.set("stats.$key.wrong", stats.wrong)
            config.set("stats.$key.currentStreak", stats.currentStreak)
            config.set("stats.$key.highestStreak", stats.highestStreak)
        }
        config.save(statsFile)
    }

    fun loadPlayer(uuid: UUID): CompletableFuture<PlayerStats?> {
        return CompletableFuture.supplyAsync {
            if (!settings.useMySQL) return@supplyAsync null
            dataSource?.connection?.use { connection ->
                connection.prepareStatement("SELECT answered, correct, wrong, current_streak, highest_streak FROM cyuquiz_stats WHERE uuid = ?").use { statement ->
                    statement.setString(1, uuid.toString())
                    val resultSet = statement.executeQuery()
                    if (resultSet.next()) {
                        return@supplyAsync PlayerStats(
                            resultSet.getInt("answered"),
                            resultSet.getInt("correct"),
                            resultSet.getInt("wrong"),
                            resultSet.getInt("current_streak"),
                            resultSet.getInt("highest_streak")
                        )
                    }
                }
            }
            null
        }
    }

    fun savePlayer(uuid: UUID, stats: PlayerStats) {
        CompletableFuture.runAsync {
            savePlayerSync(uuid, stats)
        }
    }

    fun savePlayerSync(uuid: UUID, stats: PlayerStats) {
        if (!settings.useMySQL) return
        dataSource?.connection?.use { connection ->
            connection.prepareStatement(
                "INSERT INTO cyuquiz_stats (uuid, answered, correct, wrong, current_streak, highest_streak) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "answered = VALUES(answered), correct = VALUES(correct), wrong = VALUES(wrong), " +
                        "current_streak = VALUES(current_streak), highest_streak = VALUES(highest_streak)"
            ).use { statement ->
                statement.setString(1, uuid.toString())
                statement.setInt(2, stats.answered)
                statement.setInt(3, stats.correct)
                statement.setInt(4, stats.wrong)
                statement.setInt(5, stats.currentStreak)
                statement.setInt(6, stats.highestStreak)
                statement.executeUpdate()
            }
        }
    }

    fun getTopCorrect(limit: Int): CompletableFuture<List<Pair<UUID, Int>>> {
        return CompletableFuture.supplyAsync {
            val topList = mutableListOf<Pair<UUID, Int>>()
            if (!settings.useMySQL) return@supplyAsync topList
            dataSource?.connection?.use { connection ->
                connection.prepareStatement("SELECT uuid, correct FROM cyuquiz_stats ORDER BY correct DESC LIMIT ?").use { statement ->
                    statement.setInt(1, limit)
                    val resultSet = statement.executeQuery()
                    while (resultSet.next()) {
                        val parsedUuid = try {
                            UUID.fromString(resultSet.getString("uuid"))
                        } catch (_: IllegalArgumentException) {
                            continue
                        }
                        topList.add(parsedUuid to resultSet.getInt("correct"))
                    }
                }
            }
            topList
        }
    }
}
