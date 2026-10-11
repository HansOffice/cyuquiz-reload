# CyuQuiz-Reload

Cyu 系列互动问答与定时答题活动插件

使用说明见 [`用户文档/cyuquiz-reload文档.html`](用户文档/cyuquiz-reload文档.html)

## 运行包

| 服务端 | 使用文件 |
|---|---|
| Paper 1.21+ | `cyuquiz-reload-paper-1.0.4.jar` |
| Folia 1.21+ | `cyuquiz-reload-folia-1.0.4.jar` |
| Spigot 1.16.5~1.20.x | `cyuquiz-reload-legacy-1.0.4.jar` |

每台服务端按类型放一个运行包到 `plugins` 即可，首次启动自动生成配置

## 常用入口

| 命令 | 用途 |
|---|---|
| `/cyuquiz help [页码]` | 查看帮助说明（条目可点击回填，按权限过滤） |
| `/cyuquiz answer <答案>` | 回答当前问题 |
| `/cyuquiz stats` | 查看个人答题统计 |
| `/cyuquiz top` | 查看答题正确数排行榜 |
| `/cyuquiz bank list` | 查看所有已加载题库与题目统计 |
| `/cyuquiz reload` | 重载配置文件与题库 |
| `/cyuquiz event list` | 查看所有配置的答题活动 |
| `/cyuquiz event status` | 查看当前正在进行的活动状态 |
| `/cyuquiz event start <活动ID>` | 手动开启指定答题活动 |
| `/cyuquiz event stop` | 手动终止当前进行中的答题活动 |

| 权限 | 用途 |
|---|---|
| `cyuquiz.use` | 查看帮助、统计、排行榜、题库列表与回答问题 |
| `cyuquiz.reload` | 重载配置文件与题库 |
| `cyuquiz.event.admin` | 管理答题活动（开启/停止/查看） |

## 配置文件

| 文件 / 目录 | 用途 |
|---|---|
| `config.yml` | 出题间隔、智能休眠、奖池滚存、阶梯名次、答题段位、全服狂热、防脚本与定时活动 |
| `quizzes/` | 题库配置目录，每个 `.yml` 代表一个独立题库（如 `minecraft.yml`、`general.yml`） |
| `stats.yml` | 本地玩家答题统计数据（未开启 MySQL 时使用） |
| `lang/` | 多语言配置目录（包含 `zh_cn.yml` 与 `en_us.yml`） |
| `backup/` | 覆盖前自动备份与升级备份 |

## 变量支持 (PlaceholderAPI)

| 占位符 | 说明 |
|---|---|
| `%cyuquiz_answered%` | 玩家总答题数量 |
| `%cyuquiz_correct%` | 玩家答对的题目数量 |
| `%cyuquiz_wrong%` | 玩家答错的题目数量 |
| `%cyuquiz_current_streak%` | 玩家当前连对题目数量 |
| `%cyuquiz_highest_streak%` | 玩家最高连对题目数量 |
| `%cyuquiz_accuracy%` | 玩家答题正确率百分比 |
| `%cyuquiz_rank%` | 玩家当前答题荣誉段位称号 |
| `%cyuquiz_rank_next%` | 晋级下一段位所需答对题数 |
| `%cyuquiz_jackpot%` | 当前滚存奖池倍率 |
| `%cyuquiz_fever%` | 当前是否处于答题狂热时刻（返回 是 / 否） |

## 源码结构

```text
src/
├─ main/kotlin/org/cyuCBMclean/cyuquizReload/
│  ├─ api/          公共 API 接口、默认实现、Provider 门面与生命周期事件
│  ├─ bridge/papi/  PlaceholderAPI 变量注册扩展
│  ├─ command/      指令路由、Tab 补全与 250ms 点击防抖
│  │  └─ sub/       帮助行模型与可点击分页帮助渲染器
│  ├─ config/       强类型配置读取、多语言管理与配置自动备份升级
│  ├─ listener/     异步聊天答题拦截、进服同步与退服保存注销
│  ├─ model/        题库、题目、难度、段位与活动数据模型
│  ├─ platform/     多平台调度器抽象接口
│  ├─ service/      答题核心逻辑、奖励发放、并发安全统计与防脚本服务
│  ├─ storage/      多文件题库目录加载平滑迁移与本地/MySQL 持久化
│  └─ util/         渐变色标文本引擎、资源解压、音效与分批 BossBar 视觉工具
│
├─ paper/           Paper 1.21+ 单主线程调度与平台标识
├─ folia/           Folia 1.21+ 区域多线程调度与平台标识
└─ legacy/          Spigot 1.16.5~1.20.x 经典端兼容调度与平台标识
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

构建产物分别位于各配置对应的 `target/` 目录

## 许可

本项目使用仓库内的 [`LICENSE`](LICENSE)，采用 MIT 协议开源
