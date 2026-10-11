# CyuQuiz-Reload

Cyu 系列互动问答与定时答题活动插件。

支持多文件独立题库目录、世界空间感知出题、原版音效线索题、奖池滚存累加、前三名阶梯领奖、全服狂热时刻、答题段位晋升、低在线人数智能休眠、自动定时出题、多世界黑白名单、连对奖励、定时活动排期与排名奖励、防脚本秒答刷题防护，支持 Paper / Folia / Legacy 三端原生分流。

## 常用入口

| 命令 | 用途 | 默认赋予 |
| --- | --- | --- |
| `/cyuquiz help [页码]` | 查看帮助说明（条目可点击回填，按权限过滤） | 全体玩家 |
| `/cyuquiz answer <答案>` | 回答当前问题 | 全体玩家 |
| `/cyuquiz stats` | 查看个人答题统计 | 全体玩家 |
| `/cyuquiz top` | 查看答题正确数排行榜 | 全体玩家 |
| `/cyuquiz bank list` | 查看所有已加载题库与题目统计 | 全体玩家 |
| `/cyuquiz reload` | 重载配置文件与题库 | OP |
| `/cyuquiz event list` | 查看所有配置的答题活动 | OP |
| `/cyuquiz event status` | 查看当前正在进行的活动状态 | OP |
| `/cyuquiz event start <活动ID>` | 手动开启指定答题活动 | OP |
| `/cyuquiz event stop` | 手动终止当前进行中的答题活动 | OP |

| 权限 | 用途 | 默认赋予 |
| --- | --- | --- |
| `cyuquiz.use` | 查看帮助、统计、排行榜、题库列表与回答问题 | 全体玩家 |
| `cyuquiz.reload` | 重载配置文件与题库 | OP |
| `cyuquiz.event.admin` | 管理答题活动（开启/停止/查看） | OP |

## 配置文件

| 文件 / 目录 | 用途 |
| --- | --- |
| `config.yml` | 出题间隔、智能休眠、奖池滚存、阶梯名次、答题段位、全服狂热、防脚本与定时活动 |
| `quizzes/` | 题库配置目录，每个 `.yml` 代表一个独立题库（如 `minecraft.yml`、`general.yml`） |
| `stats.yml` | 本地玩家答题统计数据（未开启 MySQL 时使用） |
| `lang/` | 多语言配置目录（包含 `zh_cn.yml` 与 `en_us.yml`） |
| `backup/` | 覆盖前自动备份与升级备份 |

## 变量支持 (PlaceholderAPI)

| 占位符 | 说明 |
| --- | --- |
| `%cyuquiz_answered%` | 玩家总答题数量 |
| `%cyuquiz_correct%` | 玩家答对的题目数量 |
| `%cyuquiz_wrong%` | 玩家答错的题目数量 |
| `%cyuquiz_current_streak%` | 玩家当前连对题目数量 |
| `%cyuquiz_highest_streak%` | 玩家最高连对题目数量 |
| `%cyuquiz_accuracy%` | 玩家答题正确率百分比 |
| `%cyuquiz_rank%` | 玩家当前答题荣誉段位称号 |
| `%cyuquiz_rank_next%` | 晋级下一段位所需答对题数 |
| `%cyuquiz_jackpot%` | 当前滚存奖池倍率 |
| `%cyuquiz_fever%` | 当前是否处于答题狂热时刻（是/否） |

## 源码结构

```text
src/main/kotlin/org/cyuCBMclean/cyuquizReload/
├─ api/          CyuQuizApi 接口、默认实现、CyuQuizProvider 门面与生命周期事件
├─ bridge/       外部桥接
│  └─ papi/      PlaceholderAPI 变量注册扩展 (CyuQuizPlaceholderExpansion)
├─ command/      指令路由、Tab 补全、250ms 点击防抖
│  └─ sub/       帮助行模型与可点击分页帮助渲染器 (HelpRenderer)
├─ config/       强类型配置读取 (Settings)、多语言管理 (MessageManager) 与自动升级备份 (ConfigUpgradeManager)
├─ listener/     聊天答题、进服同步与退服保存监听器
├─ model/        题库 (QuizBank)、难度、题型、题目数据模型与活动状态模型
├─ platform/     多平台调度器抽象接口 (QuizTaskRunner)
├─ service/      答题核心服务、奖励发放、并发安全统计服务与防脚本服务 (AntiBotService)
├─ storage/      多文件题库加载与平滑迁移 (QuestionCatalog) 与玩家统计持久化 (StatsStorage)
└─ util/         渐变与色标文本引擎 (Text)、资源解压工具 (ResourceFiles)、音效工具与视觉工具 (VisualUtil)

src/{paper,folia,legacy}/kotlin/org/cyuCBMclean/cyuquizReload/platform/
├─ PlatformHooks.kt       各端标识 (paper / folia / legacy)
└─ PlatformTaskRunner.kt  各端原生线程调度实现 (RegionScheduler / BukkitScheduler)
```

## 构建

```bash
# 构建 Paper 1.21+ 运行包（默认）
mvn clean package -Ppaper -DskipTests

# 构建 Folia 1.21+ 运行包
mvn package -Pfolia -DskipTests

# 构建 Spigot 1.16-1.20 运行包
mvn package -Plegacy -DskipTests
```

构建产物位于：
- `target/cyuquiz-reload-paper-1.0.4.jar`
- `target/cyuquiz-reload-folia-1.0.4.jar`
- `target/cyuquiz-reload-legacy-1.0.4.jar`
