# AutoAttackCooldown — Fabric 1.21.11 → Forge 1.20.1 移植报告

> ✅ **构建成功** — `BUILD SUCCESSFUL in 1m 14s`

## 概述

成功将 Fabric 1.21.11 的自动攻击冷却模组完整移植到 Forge 1.20.1 平台。保留所有核心功能和行为逻辑。

## 交付物

### 📦 JAR 产物
`D:\My_Project\minecraft_mod\Auto Attack Cooldown\1.20.1_Forge\autoattack-mod\build\libs\AutoAttackCooldown-1.0.0.jar`（104KB）

### 📁 完整源码
`D:\My_Project\minecraft_mod\Auto Attack Cooldown\1.20.1_Forge\autoattack-mod\`

```
1.20.1_Forge/autoattack-mod/
├── build.gradle                          # ForgeGradle 6.x + Mixin 插件
├── settings.gradle                       # Forge + Sponge Maven 仓库
├── gradle.properties                     # 1.20.1 + Forge 47.3.7 + Java 17
└── src/main/
    ├── java/dev/gamebuddy/autoattack/
    │   ├── AutoAttackClient.java         # @Mod 主入口
    │   ├── AttackController.java         # 核心逻辑（Forge 事件总线）
    │   ├── AutoAttackConfig.java         # 配置管理（混合架构）
    │   ├── AutoAttackConfigSpec.java     # ForgeConfigSpec 定义
    │   └── mixin/
    │       └── ClientPlayerInteractionManagerMixin.java
    └── resources/
        ├── META-INF/mods.toml
        ├── autoattack.mixins.json
        ├── pack.mcmeta
        ├── autoattack.png (logo)
        └── assets/autoattack/
            ├── icon.png
            └── lang/{en_us,zh_cn}.json
```

## 移植核心变化

### 构建系统
| Fabric (1.21.11) | Forge (1.20.1) |
|---|---|
| Fabric Loom 1.13.1 + Java 21 | ForgeGradle 6.x + Java 17 |
| Yarn 映射 | Mojang Official 映射 |
| fabric.mod.json | mods.toml |
| `ClientModInitializer` | `@Mod` + `FMLClientSetupEvent` |

### API 映射
| Fabric (Yarn) | Forge (Mojang) |
|---|---|
| `ClientTickEvents.END_CLIENT_TICK` | `TickEvent.ClientTickEvent` (Phase.END) |
| `KeyBindingHelper.registerKeyBinding()` | `RegisterKeyMappingsEvent` |
| `KeyBinding` / `KeyBinding.Category` | `KeyMapping` / String category |
| `client.options.attackKey` | `mc.options.keyAttack` |
| `client.crosshairTarget` | `mc.hitResult` |
| `client.interactionManager` | `mc.gameMode` (MultiPlayerGameMode) |
| `player.getAttackCooldownProgress()` | `player.getAttackStrengthScale()` |
| `player.resetTicksSince()` | `player.resetAttackStrengthTicker()` |
| `interactionManager.attackEntity()` | `gameMode.attack()` |
| `player.getEntityInteractionRange()` | `gameMode.getPickRange()` |
| `Text.translatable()` | `Component.translatable()` |
| `BlockHitResult.createMissed()` | `BlockHitResult.miss()` |
| `BlockPos.ofFloored()` | `BlockPos.containing()` |
| `Box.raycast()` | `AABB.clip()` |

### Mixin 适配
- 目标类：`ClientPlayerInteractionManager` → **`MultiPlayerGameMode`**
- 方法名：`attackBlock` / `updateBlockBreakingProgress` → **`startDestroyBlock` / `continueDestroyBlock`**

### 配置系统
- Fabric：手写 Gson JSON + ModMenu + Cloth Config
- Forge：**ForgeConfigSpec**（内置 GUI）+ **辅助 JSON**（运行时 `enabled` 切换持久化）

### 构建注意事项
- Gradle `org.gradle.java.home` 指向本地 Java 17，所有依赖都在项目局部安装（Gradle Cache）
- 未安装任何全局 JDK，使用 PyCharm 自带的 JBR 17

## 保留的原始功能

- ✅ 自动攻击（冷却就绪时出手）
- ✅ 动态武器攻速适配（`getAttackStrengthScale()`）
- ✅ 两阶段穿透方块实体检测（AABB clip + 20° 角度降级）
- ✅ 阻止方块破坏（Mixin 拦截）
- ✅ 空气/方块空气挥击保持节奏
- ✅ Punchy 动画兼容（临时修改 `mc.hitResult`）
- ✅ J 键切换开关 + actionbar 提示
- ✅ 纯客户端（单人/多人通用）
- ✅ 中英文本地化
