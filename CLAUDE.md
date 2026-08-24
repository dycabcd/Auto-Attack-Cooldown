# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

AutoAttackCooldown 是一个 Minecraft 客户端模组：长按攻击键时，在武器冷却就绪的瞬间自动出手，最大化 DPS。纯客户端实现，单人和多人通用，发出的网络包与手动点击一致（无作弊特征）。

仓库采用**多平台单仓库**结构，每个 Minecraft 版本/模组加载器一个子目录：

| 目录 | 平台 | MC 版本 | 映射 | Java | 状态 |
| --- | --- | --- | --- | --- | --- |
| `1.20.1_Forge/autoattack-mod/` | Forge 47.3.7 | 1.20.1 | Mojang Official | 17 | 完整 |
| `1.21.11_Fabric/autoattack-mod/` | Fabric (Loom) | 1.21.11 | Yarn | 21 | 完整 |
| `1.21.1_NeoForge/autoattack-mod/` | NeoForge 21.1.x | 1.21.1 | Mojang Official | 21 | 完整 |

三套源码包名一致（`dev.gamebuddy.autoattack`），但因映射与 API 不同，**各版本的类名/方法名不通用**，修改时必须明确针对哪个版本，不要假设跨版本可移植。

## 构建与运行

每个子工程都是独立的 Gradle 工程，自带 `gradlew`（NeoForge 子目录同样自带）。在各子目录下执行：

```bash
cd "1.20.1_Forge/autoattack-mod"      # 或 1.21.11_Fabric/autoattack-mod，或 1.21.1_NeoForge/autoattack-mod
./gradlew build          # 产物在 build/libs/（*.jar）
./gradlew runClient      # 启动开发环境客户端实测
```

**环境注意**：
- `1.20.1_Forge/.../gradle.properties` 中 `org.gradle.java.home` 硬编码了作者的本地 JDK 路径（`D:\ProgramData\PyCharm\...`）。换机或换人开发时需改为本机 JDK 17 路径，或注释掉该行让 Gradle 从 PATH 解析。toolchain 已配置为 Java 17。NeoForge 版与 Fabric 版已将该行注释为默认，靠 toolchain 解析 JDK 21/21。
- Windows 下若 Gradle 出现 SSL 握手问题，需加 JVM 参数 `-Djavax.net.ssl.trustStoreType=Windows-ROOT`。
- **NeoForge 版已实测可构建**：`neoforge_version=21.1.169`、`net.neoforged.moddev` 插件 `1.0.21`、Gradle 8.14、Java 21 均通过，产物 `build/libs/AutoAttackCooldown-1.0.0.jar`。
- **NeoForge 版的 Windows/国内网络构建适配**（非逻辑改动，仅为让构建跑通）：仓库 `.gitignore` 忽略 `*.jar` 致 `gradle-wrapper.jar` 缺失，需手动补入（可从 `raw.githubusercontent.com/gradle/gradle/master/gradle/wrapper/gradle-wrapper.jar` 下载）；`gradle-wrapper.properties` 的 `distributionUrl` 指向腾讯云镜像并设 `networkTimeout=180000`；`settings.gradle` 加阿里云镜像加速通用依赖；`gradle.properties` 延长 HTTP 超时；构建前需 `export JAVA_OPTS="-Djavax.net.ssl.trustStoreType=Windows-ROOT"` 让 Java 用 Windows 根证书库（解决 PKIX 证书错误，见 `DEVELOPMENT.md`）。若在国外网络环境，可把这些镜像/超时改回标准配置。
- 本仓库无单元测试，验证靠 `runClient` 实机测试。

## 核心架构

所有版本的逻辑都围绕同一个每客户端 tick 触发的判定链路（详见各目录的 `overview.md`）：

```
模组开启 → 无界面打开 → 长按攻击键 → 非旁观 → 非使用物品
→ 方块目标处理（零硬度穿透 / 固体转 MISS）→ 冷却进度 ≥ 阈值
→ 调用 attack + swingHand
```

### 关键文件（每个版本结构相同）

- **`AttackController.java`** — 核心。`onClientTick` 是整个模组的主入口，串联所有判定逻辑。包含 `findEntityThroughBlock()` 两阶段穿透检测算法。这是改动最频繁的文件。
- **`AutoAttackConfig.java`** — 配置运行时缓存。Forge 版用 `ForgeConfigSpec` 驱动（`bake()` 同步），Fabric 版用 JSON 文件（`AutoAttackConfig.load()` + `AutoAttackConfig.get()`）。两版字段集不同（Forge 多了 4 个 `penetration*` 穿草参数，Fabric 内联为常量）。
- **`AutoAttackClient.java`** — 客户端入口。Forge 版用 `@Mod` + 事件总线手动注册；Fabric 版实现 `ClientModInitializer`。
- **`mixin/ClientPlayerInteractionManagerMixin.java`** — **唯一的 Mixin**。`@Inject(cancellable)` 拦截方块破坏方法（Forge 映射 `MultiPlayerGameMode.startDestroyBlock`/`continueDestroyBlock`；Fabric 映射 `ClientPlayerInteractionManager.attackBlock`/`updateBlockBreakingProgress`）。**不调用 `cancelBlockBreaking()`/`stopDestroyBlock()`**——那会每 tick 发网络包，与 Punchy 等动画模组冲突。
- **`config/AutoAttackConfigScreen.java`** — 配置界面。Forge 用内置 GUI，Fabric 用 Cloth Config + ModMenu。
- **`compat/ModMenuIntegration.java`**（仅 Fabric）— ModMenu 集成入口。

### 必须理解的几个设计决策

1. **动态武器适配**：冷却判定只调用原版 `getAttackStrengthScale()`（Fabric）/ `getAttackStrengthScale()`（Forge），分母由 `generic.attack_speed` 属性实时计算。剑、斧、三叉戟及所有第三方模组武器自动适配——**零硬编码攻速**，不要为新武器加特殊分支。

2. **两阶段穿透检测**（`findEntityThroughBlock`）：阶段一 `AABB.clip()`/`Box.raycast()` 精确求交；阶段二用 20° 角度容差 + 距离降级匹配（仅 `LivingEntity`）。修改阈值时注意：阶段一是严格几何，阶段二是容差近似，用于实体卡入方块等边界情况。

3. **冷却计时器重置**：空气挥击分支**必须**调用 `resetAttackStrengthTicker()`（Forge）/ `resetTicksSince()`（Fabric 1.21.11）。这是限速的关键——原版 MISS 分支就这么做，不调用会导致每 tick 挥击 20 次/秒。对实体攻击时 `attack()` 内部已重置，无需额外调用。

4. **`mc.hitResult` / `client.crosshairTarget` 的临时修改**：穿透命中实体时临时设为 `EntityHitResult`，空气挥击时临时设为 `BlockHitResult.miss(...)`/`createMissed()`，挥完**立即恢复原值**。这是为了让 Punchy 等第一人称动画模组正确播放挥臂动画——改动这块代码务必保留"临时改 + 恢复"的对称结构。

5. **仅长按接管**：单击仍走原版逻辑，模组只在 `keyAttack.isDown()` 持续按下时接管节奏。判定长按时还额外用 GLFW 直接读按键状态作为兜底（处理某些键位映射场景）。

### 映射差异速查

同名概念在两套映射中的不同叫法，改代码时对号入座。**NeoForge 1.21.1 同样使用 Mojang Official 映射，其类名/方法名与下表"Forge / Mojang Official"列完全一致**——这是 NeoForge 版可直接复用 Forge 1.20.1 的 `AttackController` 与 Mixin 源码的根本原因。

| 概念 | Forge / Mojang Official | Fabric / Yarn |
| --- | --- | --- |
| 客户端交互管理器 | `MultiPlayerGameMode` | `ClientPlayerInteractionManager` |
| 开始破坏方块 | `startDestroyBlock` | `attackBlock` |
| 继续破坏方块 | `continueDestroyBlock` | `updateBlockBreakingProgress` |
| 攻击实体 | `gameMode.attack()` | `interactionManager.attackEntity()` |
| 冷却进度 | `getAttackStrengthScale(0.0F)` | `getAttackCooldownProgress(0.0F)` |
| 重置冷却计时器 | `resetAttackStrengthTicker()` | `resetTicksSince()` |
| 射线命中结果 | `mc.hitResult` | `client.crosshairTarget` |
| AABB 裁剪 | `AABB.clip()` | `Box.raycast()` |
| 挥手 | `player.swing()` | `swingHand()` |

## 修改约定

- **跨版本同步**：修一个逻辑缺陷时，考虑是否需要在另两个版本同步修。三套 `AttackController.java` 逻辑同源但映射不同，逐字比对不可行，按"判定链路"对齐逻辑即可。
- **配置项增减**：Forge 版需同时改 `AutoAttackConfigSpec.java`（定义）、`AutoAttackConfig.java`（缓存 + `bake()`）、配置界面、`lang/*.json`；Fabric 版需改 `AutoAttackConfig.java`（JSON 读写）、`fabric.mod.json`、`lang/*.json`。
- **Mixin**：新增 Mixin 类后必须注册——Forge 在 `mods.toml` + `build.gradle` 的 `mixin` 块；Fabric 在 `autoattack.mixins.json`；**NeoForge 在 `neoforge.mods.toml` 的 `[[mixins]]` 段（`config = "autoattack.mixins.json"`）**。**NeoForge 1.21.1 不读 jar manifest 的 `MixinConfigs` 属性**（这与 Forge 1.20.1 不同，manifest 里写无效），只在 mods.toml 声明才加载；漏掉则 Mixin 静默失效——表现为"开启自动攻击仍能挖方块"。ModDev 插件也不自动注册。NeoForge 1.21.1 用 Mojang 运行时映射，Mixin 的 `method` 直接写 Mojang 名即可，无需 refmap。
- 本仓库的 `overview.md`（各目录）和根 `DEVELOPMENT.md` 记录了历次修复的根因与决策，遇到行为问题时先查这两个文件，多半已有定论。
