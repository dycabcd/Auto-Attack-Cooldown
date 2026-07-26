package dev.gamebuddy.autoattack;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

/**
 * 核心控制器：每客户端 tick 检查一次。
 * 使用 YARN 映射，直接调用公开 API，不依赖 Mixin。
 */
public final class AttackController {

    private static KeyBinding toggleKey;
    private static KeyBinding.Category CATEGORY;

    private AttackController() {
    }

    public static void register() {
        // 1.21.11+ KeyBinding 构造函数第 4 个参数改为 KeyBinding.Category 对象
        CATEGORY = new KeyBinding.Category(Identifier.of("autoattack", "main"));

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autoattack.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(AttackController::tick);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null || client.world == null || client.interactionManager == null) {
            return;
        }

        while (toggleKey.wasPressed()) {
            toggle(client);
        }

        AutoAttackConfig config = AutoAttackConfig.get();
        if (!config.enabled) {
            return;
        }

        // 打开聊天栏/背包等界面时不攻击
        if (client.currentScreen != null) {
            return;
        }

        // 仅在长按攻击键时接管；单击仍走原版逻辑，互不干扰
        if (!client.options.attackKey.isPressed()) {
            return;
        }

        if (client.player.isSpectator()) {
            return;
        }

        // 进食、拉弓、举盾期间暂停
        if (config.respectUsingItem && client.player.isUsingItem()) {
            return;
        }

        HitResult target = client.crosshairTarget;
        if (target == null) {
            return;
        }

        HitResult.Type hitType = target.getType();

        // 方块处理
        // - preventBlockBreaking 开启时：Mixin 已在客户端拦截 attackBlock/updateBlockBreakingProgress，
        //   保证方块不会被破坏
        // - preventBlockBreaking 关闭时：留给原版挖掘逻辑
        if (hitType == HitResult.Type.BLOCK) {
            if (!config.preventBlockBreaking) {
                return;
            }

            // 尝试穿透方块检测实体（如高草丛、花中的敌人）
            EntityHitResult entityThroughBlock = findEntityThroughBlock(client);
            if (entityThroughBlock != null) {
                // 同时修改 client.crosshairTarget，使 Punchy 等动画模组能正确播放挥臂动画
                client.crosshairTarget = entityThroughBlock;
                target = entityThroughBlock;
                hitType = HitResult.Type.ENTITY;
            } else {
                // 没有实体 → 转为 MISS（空气挥击保持攻击节奏）
                // 不修改 client.crosshairTarget，避免影响动画模组
                hitType = HitResult.Type.MISS;
            }
        }

        boolean isEntity = hitType == HitResult.Type.ENTITY;
        boolean isMiss = hitType == HitResult.Type.MISS;

        // 仅攻击实体模式：非实体目标时跳过
        if (isMiss && config.onlyAttackEntities) {
            return;
        }

        // 读原版冷却进度：1.0 = 完全冷却。阈值越低出手越早、单次伤害越低
        float progress = client.player.getAttackCooldownProgress(0.0F);
        if (progress < (float) config.cooldownThreshold) {
            return;
        }

        if (isEntity && target instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() == null) return;
            // 对实体：interactionManager.attackEntity() 内部已调用 player.resetTicksSince()
            client.interactionManager.attackEntity(client.player, entityHit.getEntity());
            client.player.swingHand(Hand.MAIN_HAND);
        } else {
            // 空气挥击（含被 preventBlockBreaking 转化的方块）：
            // 必须调用 resetTicksSince() 重置冷却计时器，匹配原版 doAttack() 的 MISS 分支行为
            // 不调用则 ticksSinceLastAttack 永远递增，getAttackCooldownProgress() 永远为 1.0，导致每 tick 挥击
            //
            // 临时将 crosshairTarget 设为 MISS，使 Punchy 等动画模组播放挥臂动画
            // 挥完后立即恢复原值，不影响其他系统
            HitResult originalTarget = client.crosshairTarget;
            client.crosshairTarget = BlockHitResult.createMissed(
                    client.player.getCameraPosVec(1.0F),
                    Direction.UP,
                    BlockPos.ofFloored(client.player.getCameraPosVec(1.0F))
            );
            client.player.resetTicksSince();
            client.player.swingHand(Hand.MAIN_HAND);
            client.crosshairTarget = originalTarget;
        }
    }

    /**
     * 尝试穿透方块检测实体。
     * 用于高草丛、花、藤蔓等没有碰撞箱的方块，
     * 玩家准星被这些方块挡住时，仍能攻击到后面的实体。
     * <p>
     * 使用两阶段检测：
     * 1. 精确射线检测：对搜索框内每个实体做 AABB-raycast 精确求交
     * 2. 角度匹配降级：如果射线检测未命中，根据距离和方向夹角找最近的合格实体
     *
     * @return 如果找到实体则返回 EntityHitResult，否则返回 null
     */
    private static EntityHitResult findEntityThroughBlock(MinecraftClient client) {
        Vec3d start = client.player.getCameraPosVec(1.0F);
        Vec3d direction = client.player.getRotationVec(1.0F);
        double reach = client.player.getEntityInteractionRange();
        Vec3d end = start.add(direction.x * reach, direction.y * reach, direction.z * reach);

        // 大范围搜索框：覆盖从玩家到目标点的所有区域，增加 2 格余量
        double margin = 2.0;
        Box searchBox = new Box(
                Math.min(start.x, end.x) - margin,
                Math.min(start.y, end.y) - margin,
                Math.min(start.z, end.z) - margin,
                Math.max(start.x, end.x) + margin,
                Math.max(start.y, end.y) + margin,
                Math.max(start.z, end.z) + margin
        );

        // 获取所有非旁观者、可攻击的实体
        List<Entity> entities = client.world.getOtherEntities(
                client.player,
                searchBox,
                entity -> !entity.isSpectator() && entity.canHit()
        );

        // ── 阶段一：精确 AABB 射线检测 ──
        EntityHitResult preciseResult = null;
        double nearestDistSq = reach * reach;

        for (Entity entity : entities) {
            Optional<Vec3d> hitPos = entity.getBoundingBox().raycast(start, end);
            if (hitPos.isPresent()) {
                double distSq = start.squaredDistanceTo(hitPos.get());
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    preciseResult = new EntityHitResult(entity, hitPos.get());
                }
            }
        }

        if (preciseResult != null) {
            return preciseResult;
        }

        // ── 阶段二（降级）：角度匹配 ──
        // 当 entityBox.raycast() 在边界情况（实体部分卡入方块、碰撞箱极小等）
        // 未能命中时，用方向夹角 + 距离来做近似匹配
        double angleThreshold = Math.cos(Math.toRadians(20.0)); // 20° 容差
        EntityHitResult fallbackResult = null;
        nearestDistSq = reach * reach;

        for (Entity entity : entities) {
            // 只对活着的 LivingEntity 做降级检测
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.isAlive()) continue;

            Vec3d entityCenter = entity.getBoundingBox().getCenter();
            Vec3d toEntity = entityCenter.subtract(start);
            double distSq = toEntity.lengthSquared();
            if (distSq > reach * reach || distSq < 0.01) continue;

            // 检查实体方向与玩家视线方向的夹角
            Vec3d dirToEntity = toEntity.normalize();
            double dot = direction.dotProduct(dirToEntity);
            if (dot < angleThreshold) continue;

            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                fallbackResult = new EntityHitResult(entity, entityCenter);
            }
        }

        return fallbackResult;
    }

    private static void toggle(MinecraftClient client) {
        AutoAttackConfig config = AutoAttackConfig.get();
        config.enabled = !config.enabled;
        AutoAttackConfig.save();

        if (config.showToggleMessage && client.player != null) {
            Text message = Text.translatable(config.enabled
                    ? "message.autoattack.enabled"
                    : "message.autoattack.disabled");
            client.player.sendMessage(message, true);
        }
    }
}