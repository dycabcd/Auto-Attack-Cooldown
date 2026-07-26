# Auto Attack Cooldown — 开发日志

> 完整记录从原型到发布的全过程，2026-07-26

---

## 项目概要

| 项目 | 内容 |
|------|------|
| 模组名称 | Auto Attack Cooldown |
| 版本 | 1.0.0 |
| 目标平台 | Minecraft 1.21.11 + Fabric Loader ≥ 0.19.0 |
| 语言 | Java 21 |
| 类型 | 纯客户端 Fabric 模组 |
| 功能 | 长按攻击键时自动在冷却恢复瞬间出手，动态适配所有武器攻速 |

---

## 初始状态

从已有的 `autoattack-mod/` 源码工程出发，存在以下问题：

1. **回避零硬度方块功能无效** — 使用了服务端事件 `PlayerBlockBreakEvents.BEFORE`，在多人服务器不触发
2. **空气挥击冷却间隔错误** — 空气挥击时未调用 `resetTicksSince()`，导致每 tick 都挥击（20次/秒）

---

## 第一轮修复：核心问题

### 问题 1：阻止方块破坏代替无效的零硬度回避

**根因**：`PlayerBlockBreakEvents.BEFORE` 是服务端事件，纯客户端 mod 无法触发。

**修复方案**：
- 新建 Mixin 类 `ClientPlayerInteractionManagerMixin`，在客户端拦截 `attackBlock()` 和 `updateBlockBreakingProgress()`
- 配置字段 `avoidBreakingZeroHardness` → `preventBlockBreaking`（默认 `true`）
- 移除 `AutoAttackClient.java` 中无效的服务端事件注册
- 注册 Mixin 到 `fabric.mod.json`

**涉及文件**：
- `mixin/ClientPlayerInteractionManagerMixin.java`（新建）
- `autoattack.mixins.json`（新建）
- `AttackController.java`、`AutoAttackConfig.java`、`AutoAttackClient.java`
- `AutoAttackConfigScreen.java`、`zh_cn.json`、`en_us.json`、`fabric.mod.json`

### 问题 2：空气挥击冷却间隔修复

**根因**：通过 `javap` 分析 MC 1.21.11 Yarn 映射的字节码，确认原版 `MinecraftClient.doAttack()` 的 MISS 分支调用 `PlayerEntity.resetTicksSince()` 重置冷却计时器，但模组只调了 `swingHand()`。

**修复**：在 `AttackController.java` 的空气挥击分支添加 `client.player.resetTicksSince()`。

---

## 第二轮修复：Punchy 动画模组兼容性

### 问题：搭配 Punchy 时攻击指示器来回反馈但无挥臂动画

**根因分析**：
1. Mixin 中每 tick 调用 `cancelBlockBreaking()` 触发了频繁的网络包和状态重置
2. 方块目标时调用 `swingHand()` + `resetTicksSince()` 与 Punchy 的动画系统冲突

**修复**：
- Mixin 中移除 `cancelBlockBreaking()` 调用和 `@Shadow` 声明
- `AttackController.java` 中方块目标时直接 return，不再视为空气挥击

---

## 第三轮修复：穿透方块攻击实体

### 问题：实体在草丛中攻击不到

**根因**：准星被高草丛、花、藤蔓等方块挡住时，`crosshairTarget` 为 BLOCK，模组直接 return。

**修复**：添加 `findEntityThroughBlock()` 方法：
- 方块目标时手动执行实体射线检测
- 使用 `Box.raycast()` 精确检测射线路径上的实体
- 找到实体后修改 `client.crosshairTarget`（而非局部变量），使 Punchy 能正确播放动画

---

## 第四轮修复：区域攻击模式（探索 → 回滚）

### 尝试：区域攻击模式

玩家提出创建碰撞体积进行范围攻击的方案。

**实现**：
- 重构 `tick()` 为调度器，分支到 `performAreaAttack()` 或 `performRaycastAttack()`
- 新增 `areaAttack = true` 配置项
- 2.5×2.5×2.5 碰撞体积检测并攻击所有实体

### 回滚

玩家测试后要求恢复原模式。移除所有区域攻击相关代码，恢复为内联 tick 逻辑。保留 `findEntityThroughBlock()` 穿透检测。

---

## 第五轮修复：两阶段实体检测增强

### 问题：实体靠近/部分卡入方块时仍然无法攻击

**根因**：`entityBox.raycast()` 精确射线检测在实体部分卡入方块或碰撞箱极小时可能失效。

**修复**：引入两阶段检测算法：

1. **阶段一（精确）**：AABB 射线检测（原逻辑）
2. **阶段二（降级）**：20° 角度容差 + 距离匹配，仅对活着的 `LivingEntity`

**其他改进**：
- 搜索框余量从 1.0 扩大到 2.0
- 搜索框构造从 `stretch().expand()` 改为显式 `Box`

---

## 第六轮修复：方块空气挥击

### 问题：面对方块时无空气挥击，无法保持攻击节奏

**修复**：方块没找到实体时，不再 return，而是设为 MISS 类型走空气挥击逻辑。

---

## 第七轮修复：方块空气挥击无动画

### 问题：面对方块时挥击无动画（Punchy 仍看到 BLOCK 目标）

**修复**：空气挥击前临时将 `client.crosshairTarget` 换为 `BlockHitResult.createMissed()`，挥完后恢复原值。

---

## 第八轮修复：模组图标

使用 256×256 PNG 图标（一把带有闪电效果的剑），添加到 `assets/autoattack/icon.png`，`fabric.mod.json` 添加 `"icon"` 字段。

---

## 最终审计与预发布修复

全代码审查发现的问题（无运行时逻辑错误）：

| # | 严重度 | 文件 | 问题 | 修复 |
|---|--------|------|------|------|
| 1 | 🔴 严重 | `gradle.properties` | 硬编码本地 JDK 路径 | 注释掉，已有 toolchain |
| 2 | 🔴 严重 | `overview.md` | 配置字段名与代码不一致 | 全量重写 |
| 3 | 🟡 中等 | `AutoAttackConfig.java` | `load()` 每次无条件写配置文件 | 仅在需要时保存 |
| 4 | 🟡 中等 | `fabric.mod.json` | `fabric-api` 通配符 `*` | 改为 `>=0.100.0` |
| 5 | 🟡 中等 | `build.gradle` | ModMenu beta 版本 | 改为稳定版 17.0.0 |

---

## 关键技术发现

| 发现 | 详情 |
|------|------|
| Yarn 冷却方法名 | 1.21.11 中为 `PlayerEntity.resetTicksSince()`（非旧版 `resetLastAttackedTicks()`） |
| `attackEntity()` 行为 | 内部已调用 `resetTicksSince()`，实体攻击无需额外冷却重置 |
| `doAttack()` MISS 分支 | 调用 `resetTicksSince()` → `swingHand()` |
| `doAttack()` BLOCK 分支 | 调用 `attackBlock()` → `swingHand()` |
| `Box.raycast()` 局限性 | 实体部分卡入方块时可能返回空，需降级方案 |
| `client.crosshairTarget` | 公共字段，临时修改可影响动画模组 |
| Gradle SSL 问题 | Windows 需 `-Djavax.net.ssl.trustStoreType=Windows-ROOT` |

---

## 最终功能清单

| 功能 | 状态 |
|------|------|
| 自动攻击（冷却就绪时出手） | ✅ |
| 动态武器攻速适配 | ✅ |
| 两阶段穿透方块实体检测 | ✅ |
| 阻止方块破坏（Mixin拦截） | ✅ |
| 空气/方块空气挥击保持节奏 | ✅ |
| Punchy 动画兼容 | ✅ |
| ModMenu + Cloth Config 配置界面 | ✅ |
| 中英文本地化 | ✅ |
| J 键切换开关 | ✅ |
| 模组图标 (256x256) | ✅ |
| 纯客户端（单人/多人通用） | ✅ |

---

## 统计

- **总修改轮次**：9 轮
- **新建文件**：3 个（`ClientPlayerInteractionManagerMixin.java`、`autoattack.mixins.json`、`icon.png`）
- **修改文件**：10 个
- **总代码审查通过项**：所有运行时逻辑均无 Bug
- **构建验证**：全部通过，零错误

---

*完整源码见 `autoattack-mod/` 目录 · 最终 JAR 见 `autoattack-1.0.0.jar`*
