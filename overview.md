# Auto Attack Cooldown —— MC 1.21.11 Fabric 模组

## 交付物

| 文件 | 说明 |
| --- | --- |
| `autoattack-1.0.0.jar` | 可安装的模组本体（放入 `.minecraft/mods`） |
| `autoattack-mod/` | 完整源码工程（Gradle + Fabric Loom，可二次开发） |

前置要求：Minecraft 1.21.11 + Fabric Loader ≥ 0.19.0 + Fabric API ≥ 0.100.0 + Java ≥ 21（纯客户端，服务端无需安装）。

## 核心设计

**触发链路**（每客户端 tick 判定一次，全部通过才出手）：

```
模组开启 → 无界面打开 → 长按攻击键 → 非旁观 → 非进食/拉弓/举盾
→ 穿透方块检测实体（两阶段算法）→ 冷却进度 ≥ 阈值
→ 调用 interactionManager.attackEntity() + swingHand()
```

**关键实现决策**：

1. **动态武器适配**：冷却判定使用 `PlayerEntity#getAttackCooldownProgress()`，其分母由 `generic.attack_speed` 属性实时计算。剑（攻速 1.6）、斧（攻速 0.8-1.0）以及任何通过该属性定义攻速的第三方模组武器，全部自动适配——零硬编码。

2. **两阶段实体穿透检测**（解决"准星被方块挡住攻击不到"的问题）：
   - **阶段一 —— 精确 AABB 射线检测**：对搜索框内每个实体的碰撞箱做 `raycast()` 精确求交，取距离最近的命中实体。
   - **阶段二（降级）—— 角度匹配**：当实体碰撞箱极小或部分卡入方块导致阶段一未命中时，用玩家视线方向与实体中心方向的夹角（20° 容差）+ 距离来近似匹配，仅匹配存活的 `LivingEntity`。

3. **反作弊安全**：出手直接调用 `ClientPlayerInteractionManager#attackEntity()`，发出的 `PlayerInteractEntityC2SPacket` 与手动点击完全一致；频率与"盯着冷却指示器点击的人类高手"相同（剑约 1.6 次/秒），不产生异常数据包。单机/联机均为纯客户端行为。

4. **Mixin 架构**：唯一的 Mixin（`ClientPlayerInteractionManagerMixin`）使用 `@Inject` + `cancellable` 拦截 `attackBlock()` 和 `updateBlockBreakingProgress()`，在 `preventBlockBreaking=true` 时返回 `false` 阻止方块破坏。不调用 `cancelBlockBreaking()`，避免每 tick 发送网络包导致的性能浪费和动画模组冲突。

5. **Punchy 等动画模组兼容**：
   - 穿透方块命中实体时，临时将 `client.crosshairTarget` 设为 `EntityHitResult`，使动画模组能正确播放挥臂动画。
   - 空气挥击（MISS 分支）时，临时将 `crosshairTarget` 设为 `BlockHitResult.createMissed()`，挥完立即恢复原值，不影响其他系统。
   - 方块目标且未穿透到实体时，不修改 `crosshairTarget`，标记为 MISS 走空气挥击逻辑以维持攻击节奏。

6. **单击不干扰**：模组只在"长按"时接管节奏，单击仍走原版逻辑。

## 配置文件

`.minecraft/config/autoattack.json`（首次运行自动生成）：

```json
{
  "enabled": true,
  "cooldownThreshold": 1.0,
  "onlyAttackEntities": true,
  "preventBlockBreaking": true,
  "respectUsingItem": true,
  "showToggleMessage": true
}
```

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | boolean | true | 模组总开关（游戏内按 **J** 键切换并自动保存） |
| `cooldownThreshold` | double | 1.0 | 冷却进度阈值（0.2–1.0），1.0 = 满冷却才出手（伤害最大化），越低出手越早但单次伤害越低 |
| `onlyAttackEntities` | boolean | true | 是否仅攻击实体；关闭后对空气也保持挥击节奏 |
| `preventBlockBreaking` | boolean | true | 开启后，Mixin 拦截 `attackBlock()` 和 `updateBlockBreakingProgress()`，阻止所有方块破坏 |
| `respectUsingItem` | boolean | true | 进食/拉弓/举盾等使用物品时暂停自动攻击 |
| `showToggleMessage` | boolean | true | 按 J 键切换开关时在快捷栏上方显示提示 |

## 已知边界

- 冷却进度由客户端本地跟踪，与服务器存在 ping 级误差——阈值 1.0 时最坏情况单次伤害损失 <3%，无踢出风险。
- 不使用原版攻速属性、而自造冷却体系的极少数模组武器不受本模组管理（属预期行为）。
- 实体降级检测仅匹配 `LivingEntity` 子类（阶段二）；矿车、船等非生物实体不受穿透检测影响。
- 部分服务器规则禁止任何形式的自动攻击辅助，联机使用前请确认服务器规则。

## 二次开发

```bash
cd autoattack-mod
gradlew build        # 产物在 build/libs/
gradlew runClient    # 直接启动开发环境客户端测试
```
