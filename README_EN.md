[简体中文](README.md) · **English**

# CyuQuiz

[![Paper](https://img.shields.io/badge/Paper-1.21+-0284c7?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-Supported-7c3aed?style=for-the-badge)](https://papermc.io/software/folia)
[![Spigot](https://img.shields.io/badge/Spigot-1.16.5%20~%201.20.x-ed8b00?style=for-the-badge&logo=spigotmc&logoColor=white)](https://www.spigotmc.org)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-8%20%7C%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![License](https://img.shields.io/badge/License-MIT-2563eb?style=for-the-badge)](LICENSE)

Quiz and timed event plugin for Minecraft servers, supporting Paper, Folia, and Spigot on 1.16.5 ~ 1.21+, featuring categorized question banks, server-wide fever rounds, rollover jackpot pools, streak reward ladders, and rank titles

[Official Wiki](https://nexu.cyucloud.cn) · [Community Group](https://qm.qq.com/q/2sW504fPF2) · [Issue Tracker](https://github.com/HansOffice/cyuquiz-reload/issues)

---

## Downloads

| Platform | Jar File | Compatibility |
| --- | --- | --- |
| ![Paper](https://img.shields.io/badge/Paper-0284c7?style=flat-square&logo=paper&logoColor=white) | `cyuquiz-reload-paper-1.0.4.jar` | Paper and Purpur 1.21+ |
| ![Folia](https://img.shields.io/badge/Folia-7c3aed?style=flat-square) | `cyuquiz-reload-folia-1.0.4.jar` | Folia 1.21+ |
| ![Spigot](https://img.shields.io/badge/Spigot-ed8b00?style=flat-square&logo=spigotmc&logoColor=white) | `cyuquiz-reload-legacy-1.0.4.jar` | Spigot and Bukkit 1.16.5 ~ 1.20.x |

---

## Features

### Question Banks & Smart Trigger
- **Modular Banks**: Organize questions across multiple YAML files and categories
- **Smart Hibernation**: Pauses quiz broadcasts when server is empty or inactive to save chat space
- **Chat Interaction**: Answer directly in chat or click broadcast messages to autofill commands

### Jackpot & Fever Mode
- **Jackpot Rollover**: Unanswered reward multipliers accumulate to the next question
- **Fever Moments**: High player activity triggers double points and timed bonus rounds
- **Streak Rewards**: Distribute tiered bonuses based on response speed and answer streaks

### Ranks & Leaderboard
- **Rank Titles**: Earn points to unlock competitive titles displayed in chat and scoreboards
- **Global Top**: Track player rankings by correct answers, accuracy, and highest streaks
- **PlaceholderAPI**: Expose real-time player statistics and jackpot status to other plugins

---

## Commands & Permissions

| Command | Description | Permission | Default |
| --- | --- | --- | --- |
| `/cyuquiz help [page]` | View available commands | `cyuquiz.use` | Everyone |
| `/cyuquiz answer <text>` | Answer the active question | `cyuquiz.use` | Everyone |
| `/cyuquiz stats` | View personal quiz record and streak | `cyuquiz.use` | Everyone |
| `/cyuquiz top` | View correct answer rankings | `cyuquiz.use` | Everyone |
| `/cyuquiz bank list` | View loaded question banks and totals | `cyuquiz.use` | Everyone |
| `/cyuquiz event status` | Check status of active timed event | `cyuquiz.use` | Everyone |
| `/cyuquiz event list` | List all configured quiz events | `cyuquiz.event.admin` | OP |
| `/cyuquiz event start <id>` | Manually start a quiz event | `cyuquiz.event.admin` | OP |
| `/cyuquiz event stop` | Stop active quiz event | `cyuquiz.event.admin` | OP |
| `/cyuquiz reload` | Reload configuration files and question banks | `cyuquiz.reload` | OP |

---

## Placeholders (PlaceholderAPI)

| Placeholder | Description |
| --- | --- |
| `%cyuquiz_answered%` | Total questions answered by player |
| `%cyuquiz_correct%` | Total correct answers |
| `%cyuquiz_wrong%` | Total incorrect answers |
| `%cyuquiz_current_streak%` | Current consecutive correct streak |
| `%cyuquiz_highest_streak%` | Personal best consecutive correct streak |
| `%cyuquiz_accuracy%` | Answer accuracy percentage |
| `%cyuquiz_rank%` | Current quiz rank title |
| `%cyuquiz_rank_next%` | Required correct answers for next rank |
| `%cyuquiz_jackpot%` | Current rollover jackpot multiplier |
| `%cyuquiz_fever%` | Fever mode state |

---

## Configuration

- `config.yml`: Interval timers, smart hibernation, fever triggers, jackpot settings, and events
- `quizzes/`: Directory storing modular question bank YAML files
- `lang/`: Localization files with built-in `zh_cn.yml` and `en_us.yml`
- `stats.yml`: Local player statistics storage when MySQL is disabled
- `backup/`: Automatic configuration backups created before updates

---

## Building from Source

```bash
# Build Paper jar
mvn clean package -Ppaper -DskipTests

# Build Folia jar
mvn package -Pfolia -DskipTests

# Build 1.16.5~1.20.x jar
mvn package -Plegacy -DskipTests
```

---

## License

This project is licensed under the MIT License, see [`LICENSE`](LICENSE)
