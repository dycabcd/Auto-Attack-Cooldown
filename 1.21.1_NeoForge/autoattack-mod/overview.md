# AutoAttackCooldown —— MC 1.21.1 NeoForge 模组

## 交付物

| 文件 | 说明 |
| --- | --- |
| `AutoAttackCooldown-1.0.0.jar` | 可安装的模组本体（放入 `.minecraft/mods`） |
| `autoattack-mod/` | 完整源码工程（Gradle + ModDev，可二次开发） |

前置要求：Minecraft 1.21.1 + NeoForge ≥ 21.1 + Java ≥ 21（纯客户端，服务端无需安装）。

## 与其他版本的关系

本版是 AutoAttackCooldown 的 NeoForge 1.21.1 移植，功能与 Forge 1.20.1 版、Fabric 1.21.11 版完全一致：

- **映射**：NeoForge 1.21.1 使用 Mojang Official 映射，与 Forge 1.20.1 相同。因此 `AttackController` 与 `ClientPlayerInteractionManagerMixin` 的类名、方法名与 Forge 1.20.1 版几乎逐字一致，仅事件/配置相关的 `net.minecraftforge` 包名改为 `net.neoforged`。
- **逻辑核对**：参考 Fabric 1.21.11 版确认 1.21 平台的两处差异——
  1. 1.21 拆分了方块/实体交互距离，穿透检测的 reach 从 `gameMode.getPickRange()` 改为 `player.getEntityInteractionRange()`（与 Fabric 版的 `getEntityInteractionRange()` 对齐）。
  2. tick 入口从 Forge 1.20.1 的 `TickEvent.ClientTickEvent` + `phase == END` 改为 NeoForge 1.21.1 的 `ClientTickEvent.Post`。

## 核心设计

**触发链路**（每客户端 tick 判定一次，全部通过才出手）：

```
模组开启 → 无界面打开 → 长按攻击键 → 非旁观 → 非进食/拉弓/举盾
→ 穿透方块检测实体（两阶段算法）→ 冷却进度 ≥ 阈值
→ 调用 gameMode.attack() + player.swing()
```

**关键实现决策**（与 Forge/Fabric 版相同）：

1. **动态武器适配**：冷却判定使用 `PlayerEntity#getAttackStrengthScale()`，其分母由 `generic.attack_speed` 属性实时计算。剑、斧以及任何通过该属性定义攻速的第三方模组武器全部自动适配——零硬编码。

2. **两阶段实体穿透检测**（解决"准星被方块挡住攻击不到"的问题）：
   - **阶段一 —— 精确 AABB 射线检测**：对搜索框内每个实体的碰撞箱做 `AABB.clip()` 精确求交，取距离最近的命中实体。
   - **阶段二（降级）—— 角度匹配**：当实体碰撞箱极小或部分卡入方块导致阶段一未命中时，用玩家视线方向与实体中心方向的夹角（20° 容差）+ 距离来近似匹配，仅匹配存活的 `LivingEntity`。

3. **零硬度方块穿透**：只允许穿透硬度为 0 的方块（草、花、藤蔓、树苗等），固体方块（石、木、铁栅栏等）正常阻挡，转为 MISS 走空气挥击。

4. **反作弊安全**：出手直接调用 `MultiPlayerGameMode#attack()`，发出的 `ServerboundInteractPacket` 与手动点击完全一致；频率与"盯着冷却指示器点击的人类高手"相同（剑约 1.6 次/秒），不产生异常数据包。单机/联机均为纯客户端行为。

5. **Mixin 架构**：唯一的 Mixin（`ClientPlayerInteractionManagerMixin`）使用 `@Inject` + `cancellable` 拦截 `startDestroyBlock()` 和 `continueDestroyBlock()`，在 `preventBlockBreaking=true` 时返回 `false` 阻止方块破坏。不调用 `stopDestroyBlock()`，避免每 tick 发送网络包导致的性能浪费和动画模组冲突。

6. **Punchy 等动画模组兼容**：
   - 穿透方块命中实体时，临时将 `mc.hitResult` 设为 `EntityHitResult`，使动画模组能正确播放挥臂动画。
   - 空气挥击（MISS 分支）时，临时将 `hitResult` 设为 `BlockHitResult.miss()`，挥完立即恢复原值，不影响其他系统。
   - 方块目标且未穿透到实体时，不修改 `hitResult`，标记为 MISS 走空气挥击逻辑以维持攻击节奏。

7. **单击不干扰**：模组只在"长按"时接管节奏，单击仍走原版逻辑。

## 配置文件

`.minecraft/config/autoattack.toml`（首次运行自动生成）：

| 字段 | 类型 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | boolean | true | 模组总开关（游戏内按 **J** 键切换并自动保存） |
| `cooldownThreshold` | double | 1.0 | 冷却进度阈值（0.2–1.0），1.0 = 满冷却才出手（伤害最大化） |
| `onlyAttackEntities` | boolean | false | 是否仅攻击实体；关闭后对空气也保持挥击节奏 |
| `preventBlockBreaking` | boolean | true | 开启后，Mixin 拦截 `startDestroyBlock()` 和 `continueDestroyBlock()`，阻止所有方块破坏 |
| `respectUsingItem` | boolean | true | 进食/拉弓/举盾等使用物品时暂停自动攻击 |
| `showToggleMessage` | boolean | true | 按 J 键切换开关时在快捷栏上方显示提示 |
| `penetrationEnabled` | boolean | true | 穿草攻击总开关 |
| `penetrationAngle` | double | 20.0 | 角度匹配容差（5°~45°） |
| `penetrationMargin` | double | 2.0 | 搜索框扩展余量（0.5~5.0 格） |
| `penetrationDamageOnly` | boolean | true | 降级检测仅匹配 LivingEntity（跳过矿车/船等） |

## 二次开发

```bash
cd autoattack-mod
./gradlew build        # 产物在 build/libs/
./gradlew runClient    # 直接启动开发环境客户端测试
```

环境要求：JDK 21、Gradle 8.14+（Wrapper 已包含）。`gradle.properties` 中 `org.gradle.java.home` 默认注释，由 toolchain 解析 JDK 21；如需指定本地 JDK 路径，取消注释并填写。
