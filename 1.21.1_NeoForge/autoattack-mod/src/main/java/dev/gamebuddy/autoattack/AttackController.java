package dev.gamebuddy.autoattack;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;

/**
 * 核心控制器：每客户端 tick 检查一次。
 * 使用 Mojang Official 映射，适配 NeoForge 1.21.1。
 * <p>
 * 与 Forge 1.20.1 版逻辑同源，仅两点 1.21 平台差异：
 * 1. tick 入口改为无参（由 {@link AutoAttackClient} 注册到 {@code ClientTickEvent.Post}）
 * 2. 1.21 拆分了方块/实体交互距离，穿透检测的 reach 改用
 *    {@code player.entityInteractionRange()}（原 1.20.1 用 {@code gameMode.getPickRange()}）
 */
public final class AttackController {

    /**
     * 开关按键（J 键）
     */
    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.autoattack.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.category.autoattack.main"
    );

    private AttackController() {
    }

    /**
     * 每客户端 tick 末尾回调（由 AutoAttackClient 注册到 ClientTickEvent.Post）。
     */
    public static void onClientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return;
        }

        // 检测按键切换
        while (TOGGLE_KEY.consumeClick()) {
            toggle(mc);
        }

        if (!AutoAttackConfig.enabled) {
            return;
        }

        // 打开聊天栏/背包等界面时不攻击
        if (mc.screen != null) {
            return;
        }

        // 仅在长按攻击键时接管；单击仍走原版逻辑，互不干扰
        boolean attackHeld = mc.options.keyAttack.isDown();
        if (!attackHeld) {
            long windowHandle = mc.getWindow().getWindow();
            if (windowHandle != -1L) {
                InputConstants.Key mappingKey = mc.options.keyAttack.getKey();
                if (mappingKey.getType() == InputConstants.Type.KEYSYM) {
                    attackHeld = GLFW.glfwGetKey(windowHandle, mappingKey.getValue()) == GLFW.GLFW_PRESS;
                } else {
                    attackHeld = GLFW.glfwGetMouseButton(windowHandle, mappingKey.getValue()) == GLFW.GLFW_PRESS;
                }
            }
            if (!attackHeld) {
                return;
            }
        }

        if (mc.player.isSpectator()) {
            return;
        }

        // 进食、拉弓、举盾期间暂停
        if (AutoAttackConfig.respectUsingItem && mc.player.isUsingItem()) {
            return;
        }

        HitResult target = mc.hitResult;
        if (target == null) {
            return;
        }

        HitResult.Type hitType = target.getType();

        // ── 方块处理 ──
        // 仅当 preventBlockBreaking 开启时我们干预方块目标。
        // 然后尝试穿草攻击：只允许穿透零硬度方块（草、花、藤蔓等），
        // 固体方块（石、木等）禁止穿透。
        if (hitType == HitResult.Type.BLOCK) {
            if (!AutoAttackConfig.preventBlockBreaking) {
                return;
            }

            if (AutoAttackConfig.penetrationEnabled && target instanceof BlockHitResult blockHit) {
                // ── 零硬度方块检查 ──
                // 仅零硬度方块（草、花、藤蔓等才允许穿透）；
                // 额外排除 isAir()（结构空位、屏障等破坏未加载区域保护）
                BlockPos hitPos = blockHit.getBlockPos();
                BlockState hitState = mc.level.getBlockState(hitPos);
                float blockHardness = hitState.getDestroySpeed(mc.level, hitPos);
                if (hitState.isAir() || blockHardness > 0.0F) {
                    // 固体方块或空气 → 不穿透，直接转为 MISS 保持攻击节奏
                    hitType = HitResult.Type.MISS;
                } else {
                    // 零硬度方块 → 尝试穿透检测后方实体
                    EntityHitResult entityThroughBlock = findEntityThroughBlock(mc);
                    if (entityThroughBlock != null) {
                        mc.hitResult = entityThroughBlock;
                        target = entityThroughBlock;
                        hitType = HitResult.Type.ENTITY;
                    } else {
                        hitType = HitResult.Type.MISS;
                    }
                }
            } else {
                hitType = HitResult.Type.MISS;
            }
        }

        boolean isEntity = hitType == HitResult.Type.ENTITY;
        boolean isMiss = hitType == HitResult.Type.MISS;

        // 仅攻击实体模式：非实体目标时跳过
        if (isMiss && AutoAttackConfig.onlyAttackEntities) {
            return;
        }

        // 读原版冷却进度：1.0 = 完全冷却。阈值越低出手越早、单次伤害越低
        float progress = mc.player.getAttackStrengthScale(0.0F);
        if (progress < (float) AutoAttackConfig.cooldownThreshold) {
            return;
        }

        if (isEntity && target instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() == null) return;
            // 对实体：gameMode.attack() 内部已调用 player.resetAttackStrengthTicker()
            mc.gameMode.attack(mc.player, entityHit.getEntity());
            mc.player.swing(InteractionHand.MAIN_HAND);
        } else {
            // 空气挥击（含被 preventBlockBreaking 转化的方块和固体方块）：
            //
            // 与原版行为一致：
            //   - 对空气挥击时调用 player.resetAttackStrengthTicker() 重置冷却计时器
            //   - 冷却检查 (getAttackStrengthScale < threshold) 在下一 tick 拦截过快的后续挥击
            //   - 从而自然实现按武器冷却周期的限速，无须额外的计数器/计时器
            //
            // 此举也确保"仅攻击实体"模式 (onlyAttackEntities=false) 下
            // 面对空气时能持续维持攻击节奏，而非只挥一次就停止。
            //
            // 临时将 mc.hitResult 设为 MISS，使 Punchy 等动画模组播放挥臂动画
            HitResult originalTarget = mc.hitResult;
            mc.hitResult = BlockHitResult.miss(
                    mc.player.getEyePosition(1.0F),
                    Direction.UP,
                    BlockPos.containing(mc.player.getEyePosition(1.0F))
            );
            mc.player.resetAttackStrengthTicker();
            mc.player.swing(InteractionHand.MAIN_HAND);
            mc.hitResult = originalTarget;
        }
    }

    /**
     * 尝试穿透零硬度方块检测后方实体。
     * 用于高草丛、花、藤蔓等没有碰撞箱的方块，
     * 玩家准星被这些方块挡住时，仍能攻击到后面的实体。
     * <p>
     * 使用两阶段检测：
     * 1. 精确射线检测：对搜索框内每个实体做 AABB-raycast 精确求交
     * 2. 角度匹配降级：如果射线检测未命中，根据距离和方向夹角找最近的合格实体
     *
     * @return 如果找到实体则返回 EntityHitResult，否则返回 null
     */
    private static EntityHitResult findEntityThroughBlock(Minecraft mc) {
        Vec3 start = mc.player.getEyePosition(1.0F);
        Vec3 direction = mc.player.getViewVector(1.0F);
        // ★ 1.21 拆分了交互距离：实体穿透检测使用实体交互距离而非方块挖掘距离
        //    Mojang 映射 1.21.1 中方法名为 entityInteractionRange()（无 get 前缀）
        double reach = mc.player.entityInteractionRange();
        Vec3 end = start.add(direction.x * reach, direction.y * reach, direction.z * reach);

        // 搜索框余量（可配置）
        double margin = AutoAttackConfig.penetrationMargin;
        AABB searchBox = new AABB(
                Math.min(start.x, end.x) - margin,
                Math.min(start.y, end.y) - margin,
                Math.min(start.z, end.z) - margin,
                Math.max(start.x, end.x) + margin,
                Math.max(start.y, end.y) + margin,
                Math.max(start.z, end.z) + margin
        );

        // 获取所有非旁观者、可攻击的实体
        List<Entity> entities = mc.level.getEntities(
                mc.player,
                searchBox,
                entity -> !entity.isSpectator() && entity.isPickable()
        );

        // ── 阶段一：精确 AABB 射线检测 ──
        EntityHitResult preciseResult = null;
        double nearestDistSq = reach * reach;

        for (Entity entity : entities) {
            Optional<Vec3> hitPos = entity.getBoundingBox().clip(start, end);
            if (hitPos.isPresent()) {
                double distSq = start.distanceToSqr(hitPos.get());
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
        // 当 AABB.clip() 在边界情况（实体部分卡入方块、碰撞箱极小等）
        // 未能命中时，用方向夹角 + 距离来做近似匹配
        double angleThreshold = Math.cos(Math.toRadians(AutoAttackConfig.penetrationAngle));
        EntityHitResult fallbackResult = null;
        nearestDistSq = reach * reach;

        for (Entity entity : entities) {
            // 如果配置为仅攻击 LivingEntity，则跳过非生物实体
            if (AutoAttackConfig.penetrationDamageOnly && !(entity instanceof LivingEntity)) continue;
            // 只对活着的实体做降级检测
            if (entity instanceof LivingEntity living && !living.isAlive()) continue;

            Vec3 entityCenter = entity.getBoundingBox().getCenter();
            Vec3 toEntity = entityCenter.subtract(start);
            double distSq = toEntity.lengthSqr();
            if (distSq > reach * reach || distSq < 0.01) continue;

            // 检查实体方向与玩家视线方向的夹角
            Vec3 dirToEntity = toEntity.normalize();
            double dot = direction.dot(dirToEntity);
            if (dot < angleThreshold) continue;

            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                fallbackResult = new EntityHitResult(entity, entityCenter);
            }
        }

        return fallbackResult;
    }

    private static void toggle(Minecraft mc) {
        AutoAttackConfig.toggle();

        if (AutoAttackConfig.showToggleMessage && mc.player != null) {
            Component message = Component.translatable(AutoAttackConfig.enabled
                    ? "message.autoattack.enabled"
                    : "message.autoattack.disabled");
            mc.player.displayClientMessage(message, true);
        }
    }
}
