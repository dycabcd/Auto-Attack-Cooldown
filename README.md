<p align="center">
  <img src="./assets/banner.png" alt="AutoAttackCooldown" width="340" />
</p>

# AutoAttackCooldown

> Minecraft 自动攻击冷却模组 —— 长按攻击键，在武器冷却就绪的瞬间自动出手，最大化 DPS。纯客户端，单人/多人通用。

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1~1.21.11-brightgreen)](https://www.minecraft.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

同一套核心逻辑，覆盖三个 Minecraft 版本/加载器平台，每个平台一个独立子工程：

| 子工程 | 平台 | MC 版本 | 映射 | Java |
| --- | --- | --- | --- | --- |
| [`1.20.1_Forge/autoattack-mod`](1.20.1_Forge/autoattack-mod) | Forge 47.3.7 | 1.20.1 | Mojang Official | 17 |
| [`1.21.11_Fabric/autoattack-mod`](1.21.11_Fabric/autoattack-mod) | Fabric Loader ≥ 0.19.0 | 1.21.11 | Yarn | 21 |
| [`1.21.1_NeoForge/autoattack-mod`](1.21.1_NeoForge/autoattack-mod) | NeoForge 21.1.x | 1.21.1 | Mojang Official | 21 |

> 三套源码包名一致（`dev.gamebuddy.autoattack`），但因映射与 API 不同，类名/方法名不通用。NeoForge 1.21.1 同样使用 Mojang Official 映射，因此其核心逻辑可直接复用 Forge 1.20.1 版，仅适配 1.21 平台差异（实体交互距离拆分、客户端 tick 事件 API）。

## 功能特性

- **按冷却自动攻击** —— 长按攻击键，模组在武器冷却就绪的瞬间自动出手，最大化 DPS。
- **动态武器适配** —— 基于原版 `getAttackStrengthScale()` / `getAttackCooldownProgress()`，剑、斧、三叉戟以及所有使用 `generic.attack_speed` 属性的第三方武器全部自动适配，零硬编码。
- **两阶段实体穿透检测** —— 实体藏在草、花、藤蔓后方时依然能攻击到，固体方块（石、木等）正常阻挡。
- **仅穿透零硬度方块** —— 尊重原版玩法，只有草、花等才允许穿透。
- **阻止方块破坏** —— 开启后通过 Mixin 拦截方块破坏方法，避免战斗中误挖方块。
- **Punchy 及动画模组兼容** —— 精细控制 `mc.hitResult` / `client.crosshairTarget`，确保第一人称动画模组能正确播放挥臂动画。
- **纯客户端** —— 服务端无需安装，单人和多人通用。
- **反作弊安全** —— 发出的网络包与手动点击完全一致，无异常数据特征。
- **可配置** —— Forge/NeoForge 版 10 项配置 + 自带 GUI；Fabric 版 6 项 + ModMenu/Cloth Config 界面。
- **切换快捷键** —— 按 `J` 键随时开关。

## 安装方法

按你使用的平台选择对应的模组 jar，放入 `.minecraft/mods/`：

| 平台 | 前置 | 模组文件 |
| --- | --- | --- |
| Forge 1.20.1 | [Minecraft Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html) ≥ 47.3.7 | `AutoAttackCooldown-1.0.0.jar`（Forge 版） |
| Fabric 1.21.11 | [Fabric Loader](https://fabricmc.net/) ≥ 0.19.0 + [Fabric API](https://modrinth.com/mod/fabric-api) ≥ 0.100.0 | `AutoAttackCooldown-1.0.0.jar`（Fabric 版） |
| NeoForge 1.21.1 | [NeoForge](https://neoforged.net/) 21.1.x | `AutoAttackCooldown-1.0.0.jar`（NeoForge 版） |

进游戏 → 模组列表 → 点击 **"配置"** 按钮调整选项（Fabric 版需安装 ModMenu + Cloth Config）。

## 配置说明

### Forge / NeoForge 版

配置存储在 `.minecraft/config/autoattack.toml`（首次运行自动生成），自带 GUI，设置实时生效。

**通用选项**

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `enabled` | boolean | true | 模组总开关（游戏内按 **J** 键切换并自动保存） |
| `cooldownThreshold` | double | 1.0 | 冷却进度阈值（0.2–1.0），1.0=满冷却才出手 |
| `onlyAttackEntities` | boolean | false | 仅攻击实体；关闭时对空气也保持挥击节奏 |
| `preventBlockBreaking` | boolean | true | 开启后阻止所有方块破坏 |
| `respectUsingItem` | boolean | true | 进食、拉弓、举盾时暂停自动攻击 |
| `showToggleMessage` | boolean | true | 按 J 键切换时显示提示 |

**穿草攻击选项**

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `penetrationEnabled` | boolean | true | 穿草攻击总开关 |
| `penetrationAngle` | double | 20.0 | 角度匹配容差（5°~45°） |
| `penetrationMargin` | double | 2.0 | 搜索框扩展余量（0.5~5.0 格） |
| `penetrationDamageOnly` | boolean | true | 降级检测仅匹配生物（跳过矿车/船等） |

### Fabric 版

配置存储在 `.minecraft/config/autoattack.json`（首次运行自动生成），通过 ModMenu + Cloth Config 提供界面。Fabric 版仅暴露 6 项通用选项；穿草攻击恒定启用，角度容差固定 20°、搜索余量固定 2.0 格（与 Forge/NeoForge 版默认值一致）。

## 工作原理

三版核心判定链路完全同源，每个客户端 tick 判定一次：

```
模组开启 → 无界面打开 → 长按攻击键 → 非旁观 → 非使用物品
→ 硬度判断（零硬度方块允许穿透）→ 两阶段实体检测 → 冷却进度 ≥ 阈值
→ 调用 attack + swing
```

### 两阶段实体检测

当准星被草、花、藤蔓等零硬度方块挡住时：

1. **阶段一 —— AABB 精确射线检测**：对范围内的每个实体碰撞箱做精确求交（Forge/NeoForge 用 `AABB.clip()`，Fabric 用 `Box.raycast()`），取距离最近的命中实体。
2. **阶段二（降级）—— 角度匹配**：当阶段一因边界情况（实体碰撞箱极小、部分卡入方块）未命中时，用方向夹角 + 距离匹配，取最近的合格实体（仅 `LivingEntity`）。

### 穿草攻击限制

只允许穿透硬度为 0 的方块（草、花、藤蔓、树苗、甘蔗、农作物等），固体方块（石、木、土、铁栅栏等）正常阻挡。

### 映射差异速查

同名概念在三套映射中的不同叫法：

| 概念 | Forge / NeoForge (Mojang) | Fabric (Yarn) |
| --- | --- | --- |
| 客户端交互管理器 | `MultiPlayerGameMode` | `ClientPlayerInteractionManager` |
| 开始破坏方块 | `startDestroyBlock` | `attackBlock` |
| 攻击实体 | `gameMode.attack()` | `interactionManager.attackEntity()` |
| 冷却进度 | `getAttackStrengthScale()` | `getAttackCooldownProgress()` |
| 重置冷却计时器 | `resetAttackStrengthTicker()` | `resetTicksSince()` |
| 射线命中结果 | `mc.hitResult` | `client.crosshairTarget` |

## 兼容性

- ✅ **剑、斧、三叉戟** —— 通过原版攻速属性完整支持
- ✅ **第三方武器** —— 所有使用 `generic.attack_speed` 属性的武器自动适配
- ✅ **Punchy** —— 挥臂动画正确播放
- ✅ **配置 GUI** —— Forge/NeoForge 内置；Fabric 经 ModMenu + Cloth Config
- ✅ **单人 & 多人** —— 纯客户端，无需服务端安装
- ❌ 自定义（非原版）冷却体系的武器 —— 不受本模组管理（设计预期）

## 从源码构建

每个子工程都是独立的 Gradle 工程，自带 `gradlew`。在各子目录下执行：

```bash
# Forge 1.20.1 版本（Java 17）
cd "1.20.1_Forge/autoattack-mod"
./gradlew build          # 产物在 build/libs/
./gradlew runClient      # 启动开发环境客户端实测

# Fabric 1.21.11 版本（Java 21）
cd "1.21.11_Fabric/autoattack-mod"
./gradlew build

# NeoForge 1.21.1 版本（Java 21）
cd "1.21.1_NeoForge/autoattack-mod"
./gradlew build
./gradlew runClient
```

**环境要求**：JDK 17（Forge）/ JDK 21（Fabric、NeoForge），Gradle 8.14+（Wrapper 已包含）。

**NeoForge 版的 Windows/国内网络构建适配**（非逻辑改动，仅为让构建跑通）：仓库 `.gitignore` 忽略 `*.jar` 致 `gradle-wrapper.jar` 需手动补入；`gradle-wrapper.properties` 的 `distributionUrl` 指向腾讯云镜像并延长 `networkTimeout`；`settings.gradle` 加阿里云镜像；`gradle.properties` 已写入 `systemProp.javax.net.ssl.trustStoreType=Windows-ROOT`（让 Java 用 Windows 根证书库，解决 PKIX 证书错误）。在国外网络环境可把这些镜像/超时改回标准配置。

## 许可证

MIT License —— 详见 [LICENSE](LICENSE) 文件。

---

*三平台同源逻辑 · Mojang Official 映射（Forge 1.20.1 / NeoForge 1.21.1）· Yarn 映射（Fabric 1.21.11）*
