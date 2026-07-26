<p align="center">
  <img src="./assets/banner.png" alt="AutoAttackCooldown" width="340" />
</p>

# AutoAttackCooldown

> Minecraft 1.21.11 Fabric 模组 —— 长按攻击键，冷却就绪自动出手。

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric%20Loader-≥0.19.0-blue)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)

## 功能特性

- **按冷却自动攻击** —— 长按攻击键，模组在武器冷却就绪的瞬间自动出手，最大化 DPS。
- **动态武器适配** —— 基于原版 `getAttackCooldownProgress()`，剑、斧以及所有使用 `generic.attack_speed` 属性的第三方武器全部自动适配。
- **两阶段实体穿透检测** —— 实体藏在草丛、花、藤蔓后方或部分卡入方块时，依然能够攻击到。
- **阻止方块破坏** —— 开启后通过 Mixin 拦截 `attackBlock()` / `updateBlockBreakingProgress()`，避免战斗中误挖方块。
- **Punchy 及动画模组兼容** —— 精细控制 `client.crosshairTarget`，确保第一人称动画模组能正确播放挥臂动画。
- **纯客户端** —— 服务端无需安装，单人和多人通用。
- **反作弊安全** —— 发出的网络包与手动点击完全一致，无异常数据特征。
- **可配置** —— 支持 ModMenu + Cloth Config，6 个配置项均可通过游戏内界面调整。
- **切换快捷键** —— 按 `J` 键随时开关。

## 安装方法

1. 安装 [Fabric Loader](https://fabricmc.net/use/)（≥ 0.19.0）
2. 安装 [Fabric API](https://modrinth.com/mod/fabric-api)（≥ 0.100.0）
3. 下载 `autoattack-1.0.0.jar`，放入 `.minecraft/mods/`
4. （可选）安装 [ModMenu](https://modrinth.com/mod/modmenu) 和 [Cloth Config](https://modrinth.com/mod/cloth-config) 以使用游戏内配置界面

## 配置说明

`.minecraft/config/autoattack.json`（首次运行自动生成）：

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | boolean | true | 模组总开关（游戏内按 **J** 键切换并自动保存） |
| `cooldownThreshold` | double | 1.0 | 冷却进度阈值（0.2–1.0），1.0 = 满冷却才出手（伤害最大化），越低出手越早但单次伤害越低 |
| `onlyAttackEntities` | boolean | true | 仅攻击实体；关闭后对空气也保持挥击节奏 |
| `preventBlockBreaking` | boolean | true | 开启后阻止所有方块破坏，避免挖矿干扰战斗 |
| `respectUsingItem` | boolean | true | 进食、拉弓、举盾时暂停自动攻击 |
| `showToggleMessage` | boolean | true | 按 J 键切换开关时在快捷栏上方显示提示 |

## 工作原理

每个客户端 tick 判定一次：

```
模组开启 → 无界面打开 → 长按攻击键 → 非旁观 → 非使用物品
→ 两阶段实体检测 → 冷却进度 ≥ 阈值
→ 调用 attackEntity() + swingHand()
```

### 两阶段实体检测

当准星被草丛、花、藤蔓等无碰撞箱方块挡住时：

1. **阶段一 —— AABB 精确射线检测**：对范围内的每个实体碰撞箱做 `Box.raycast()` 精确求交，取距离最近的命中实体。
2. **阶段二（降级）—— 角度匹配**：当阶段一因边界情况（实体碰撞箱极小、部分卡入方块）未命中时，用方向夹角（≤20° 容差）+ 距离匹配，取最近且存活的 `LivingEntity`。

## 兼容性

- ✅ **剑、斧、三叉戟** —— 通过原版攻速属性完整支持
- ✅ **第三方武器** —— 所有使用 `generic.attack_speed` 属性的武器自动适配
- ✅ **Punchy** —— 挥臂动画正确播放
- ✅ **ModMenu + Cloth Config** —— 游戏内配置界面
- ✅ **单人 & 多人** —— 纯客户端，无需服务端安装
- ❌ 自定义（非原版）冷却体系的武器 —— 不受本模组管理（设计预期）

## 已知限制

- 冷却进度由客户端本地跟踪，与服务器存在 ping 级误差（阈值 1.0 时最坏情况伤害损失 < 3%，无踢出风险）。
- 阶段二降级检测仅匹配 `LivingEntity`，矿车、船等非生物实体不受影响。
- 部分服务器禁止自动攻击类模组，联机前请确认服务器规则。

## 从源码构建

```bash
cd autoattack-mod
./gradlew build        # 产物在 build/libs/
./gradlew runClient    # 直接启动开发客户端测试
```

**环境要求**：JDK 21、Gradle 8.14+（Wrapper 已包含）

## 许可证

MIT License —— 详见 [LICENSE](LICENSE) 文件。

---

*基于 Fabric Loom · Minecraft 1.21.11 · Yarn 映射 1.21.11+build.6 · Java 21*
