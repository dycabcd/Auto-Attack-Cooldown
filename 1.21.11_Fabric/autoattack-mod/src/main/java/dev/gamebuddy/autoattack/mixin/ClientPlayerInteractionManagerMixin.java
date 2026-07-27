package dev.gamebuddy.autoattack.mixin;

import dev.gamebuddy.autoattack.AutoAttackConfig;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 当自动攻击启用且 preventBlockBreaking 为 true 时，
 * 拦截 ClientPlayerInteractionManager 的方块破坏方法。
 *
 * - attackBlock(): 阻止开始破坏方块（首次点击 + 持续按住）
 * - updateBlockBreakingProgress(): 阻止继续破坏方块（持续按住）
 *
 * 纯客户端拦截，在多人服务器上同样生效（不需要服务器安装本 mod）。
 * 不调用 cancelBlockBreaking()，避免每 tick 发送网络包和状态重置，
 * 防止与 Punchy 等第一人称动画模组产生冲突（攻击指示器来回反馈）。
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    /**
     * 拦截 attackBlock：当 auto-attack 启用且 preventBlockBreaking 开启时，
     * 阻止开始破坏方块。返回 false 表示方块不可破坏。
     */
    @Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
    private void autoattack$cancelAttackBlock(BlockPos pos, Direction direction,
                                               CallbackInfoReturnable<Boolean> cir) {
        AutoAttackConfig config = AutoAttackConfig.get();
        if (config.enabled && config.preventBlockBreaking) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 拦截 updateBlockBreakingProgress：当 auto-attack 启用且 preventBlockBreaking 开启时，
     * 阻止继续破坏方块。返回 false 表示方块不可破坏。
     */
    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
    private void autoattack$cancelUpdateBlockBreakingProgress(BlockPos pos, Direction direction,
                                                               CallbackInfoReturnable<Boolean> cir) {
        AutoAttackConfig config = AutoAttackConfig.get();
        if (config.enabled && config.preventBlockBreaking) {
            cir.setReturnValue(false);
        }
    }
}
