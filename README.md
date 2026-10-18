**简体中文** · [English](README_EN.md)

# CyuQuiz · 互动问答与答题活动

[![Paper](https://img.shields.io/badge/Paper-1.21+-0284c7?style=for-the-badge&logo=paper&logoColor=white)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-Supported-7c3aed?style=for-the-badge)](https://papermc.io/software/folia)
[![Spigot](https://img.shields.io/badge/Spigot-1.16.5%20~%201.20.x-ed8b00?style=for-the-badge&logo=spigotmc&logoColor=white)](https://www.spigotmc.org)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Java](https://img.shields.io/badge/Java-8%20%7C%2017-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org)
[![License](https://img.shields.io/badge/License-MIT-2563eb?style=for-the-badge)](LICENSE)

用于 Minecraft 服务端的互动问答与定时答题活动插件，适用于 1.16.5 ~ 1.21+，支持 Paper、Folia 与 Spigot，包含多分类题库、全服狂热时刻、奖池滚存、连对阶梯奖励与答题段位称号

[官方文档与 Wiki](https://nexu.cyucloud.cn) · [CyuCloud服主技术交流群](https://qm.qq.com/q/2sW504fPF2) · [提交问题](https://github.com/HansOffice/cyuquiz-reload/issues)

---

## 运行包

| 服务端核心 | 运行包 | 适用版本 |
| --- | --- | --- |
| ![Paper](https://img.shields.io/badge/Paper-0284c7?style=flat-square&logo=paper&logoColor=white) | `cyuquiz-reload-paper-1.0.4.jar` | Paper 及 Purpur 1.21 及以上版本 |
| ![Folia](https://img.shields.io/badge/Folia-7c3aed?style=flat-square) | `cyuquiz-reload-folia-1.0.4.jar` | Folia 1.21 及以上版本 |
| ![Spigot](https://img.shields.io/badge/Spigot-ed8b00?style=flat-square&logo=spigotmc&logoColor=white) | `cyuquiz-reload-legacy-1.0.4.jar` | Spigot 及 Bukkit 1.16.5 ~ 1.20.x 版本 |

---

## 内容介绍

### 题库与智能出题
- **独立题库**：支持单文件题库扩展与多分类题目维护
- **智能休眠**：无玩家在线或无人答题时自动暂停出题，避免刷屏卡服
- **快捷答题**：直接在聊天栏输入答案或点击文本回填命令答题

### 奖池与狂热机制
- **奖池滚存**：无人答对时奖励倍率自动累加至下道题目
- **狂热时刻**：答题活跃触发全服狂热，开启限时双倍积分与抢答
- **阶梯奖励**：根据答题速度与连对次数发放阶梯奖励

### 段位与排行榜
- **段位晋级**：累计答题积分提升段位称号，并在聊天栏与计分板展示
- **全服排行**：按正确率、答对总数与历史最高连对展示排行榜
- **变量集成**：支持 PlaceholderAPI 变量展示个人战绩与奖池状态

---

## 指令

| 指令 | 效果 | 权限 |
| --- | --- | --- |
| `/cyuquiz help [页码]` | 查看可用命令指引 | 全体玩家 |
| `/cyuquiz answer <答案>` | 回答当前正在进行的题目 | 全体玩家 |
| `/cyuquiz stats` | 查看个人答题战绩与连对统计 | 全体玩家 |
| `/cyuquiz top` | 查看全服答题正确数排行榜 | 全体玩家 |
| `/cyuquiz bank list` | 查看已加载题库与题目总数 | 全体玩家 |
| `/cyuquiz event status` | 查看当前正在进行的答题活动状态 | 全体玩家 |
| `/cyuquiz event list` | 列出所有已配置的答题活动 | 管理员 |
| `/cyuquiz event start <活动ID>` | 手动开启指定的答题活动 | 管理员 |
| `/cyuquiz event stop` | 提前终止当前正在进行的答题活动 | 管理员 |
| `/cyuquiz reload` | 重载全部配置文件与题库 | 管理员 |

---

## 变量支持 (PlaceholderAPI)

| 占位符 | 说明 |
| --- | --- |
| `%cyuquiz_answered%` | 玩家总答题数量 |
| `%cyuquiz_correct%` | 玩家答对题目总数 |
| `%cyuquiz_wrong%` | 玩家答错题目总数 |
| `%cyuquiz_current_streak%` | 玩家当前连续答对数量 |
| `%cyuquiz_highest_streak%` | 玩家历史最高连对纪录 |
| `%cyuquiz_accuracy%` | 玩家答题正确率百分比 |
| `%cyuquiz_rank%` | 玩家当前答题荣誉段位称号 |
| `%cyuquiz_rank_next%` | 晋级下一段位所需答对题数 |
| `%cyuquiz_jackpot%` | 当前滚存奖池倍率 |
| `%cyuquiz_fever%` | 当前是否处于答题狂热时刻（返回 是 / 否） |

---

## 配置

- `config.yml`：出题频率、智能休眠、狂热时刻、奖池滚存与活动调度
- `quizzes/`：多题库目录，支持独立增加与编辑 yaml 题库文件
- `lang/`：多语言提示文本，默认内置 `zh_cn.yml` 与 `en_us.yml`
- `stats.yml`：玩家答题统计本地存储（未开启 MySQL 时使用）
- `backup/`：配置自动备份与升级存档

---

## 构建

```bash
# 构建 Paper 运行包
mvn clean package -Ppaper -DskipTests

# 构建 Folia 运行包
mvn package -Pfolia -DskipTests

# 构建 1.16.5~1.20.x 运行包
mvn package -Plegacy -DskipTests
```

---

## 协议

本项目采用 MIT 开源许可协议，详情参见 [`LICENSE`](LICENSE)
