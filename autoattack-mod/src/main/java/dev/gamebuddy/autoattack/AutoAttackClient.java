package dev.gamebuddy.autoattack;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AutoAttackCooldown — 客户端模组入口。
 */
public class AutoAttackClient implements ClientModInitializer {

    public static final String MOD_ID = "autoattack";
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoAttack");

    @Override
    public void onInitializeClient() {
        AutoAttackConfig.load();
        AttackController.register();
        LOGGER.info("[AutoAttack] 已加载，当前状态：{}", AutoAttackConfig.get().enabled ? "开启" : "关闭");
    }
}