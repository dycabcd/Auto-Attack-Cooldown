package dev.gamebuddy.autoattack.mixin;

import dev.gamebuddy.autoattack.AutoAttackConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 当自动攻击启用且 preventBlockBreaking 为 true 时，
 * 拦截 MultiPlayerGameMode 的方块破坏方法。
 * <p>
 * - startDestroyBlock(): 阻止开始破坏方块（首次点击 + 持续按住）
 * - continueDestroyBlock(): 阻止继续破坏方块（持续按住）
 * <p>
 * 纯客户端拦截，在多人服务器上同样生效（不需要服务器安装本 mod）。
 * 不调用 stopDestroyBlock()，避免每 tick 发送网络包和状态重置，
 * 防止与 Punchy 等第一人称动画模组产生冲突（攻击指示器来回反馈）。
 * <p>
 * NeoForge 1.21.1 使用 Mojang Official 映射，故此处类名与方法名与
 * Forge 1.20.1 版完全一致（Yarn 映射中分别对应
 * ClientPlayerInteractionManager.attackBlock / updateBlockBreakingProgress）。
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {

    /**
     * 拦截 startDestroyBlock：当 auto-attack 启用且 preventBlockBreaking 开启时，
     * 阻止开始破坏方块。返回 false 表示方块不可破坏。
     */
    @Inject(method = "startDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void autoattack$cancelStartDestroyBlock(BlockPos pos, Direction direction,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (AutoAttackConfig.enabled && AutoAttackConfig.preventBlockBreaking) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 拦截 continueDestroyBlock：当 auto-attack 启用且 preventBlockBreaking 开启时，
     * 阻止继续破坏方块。返回 false 表示方块不可破坏。
     */
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void autoattack$cancelContinueDestroyBlock(BlockPos pos, Direction direction,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (AutoAttackConfig.enabled && AutoAttackConfig.preventBlockBreaking) {
            cir.setReturnValue(false);
        }
    }
}
