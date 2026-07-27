package dev.gamebuddy.autoattack;

import dev.gamebuddy.autoattack.config.AutoAttackConfigScreen;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AutoAttackCooldown — Forge 1.20.1 客户端模组入口。
 */
@Mod(AutoAttackClient.MOD_ID)
public class AutoAttackClient {

    public static final String MOD_ID = "autoattack";
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoAttack");

    public AutoAttackClient() {
        // 注册配置（ForgeConfigSpec）
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                AutoAttackConfigSpec.SPEC,
                "autoattack.toml"
        );

        // ★ 程序化注册配置界面（绕过 mods.toml 的 configGUI 反射）
        AutoAttackConfigScreen.register();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // ★ 在构造函数中手动注册配置事件监听，确保注册时机早于 ModConfigEvent 的触发
        modBus.addListener(AutoAttackConfig::onModConfigEvent);

        // 客户端设置
        modBus.addListener(this::onClientSetup);

        // 按键注册
        modBus.addListener(this::onRegisterKeyMappings);
    }

    private void onClientSetup(final FMLClientSetupEvent event) {
        // 做一次额外的同步以确保首次启动时配置生效
        AutoAttackConfig.bake();

        // 注册 tick 回调到 Forge 事件总线
        MinecraftForge.EVENT_BUS.addListener(AttackController::onClientTick);

        LOGGER.info("[AutoAttack] 已加载，当前状态：{}", AutoAttackConfig.enabled ? "开启" : "关闭");
    }

    private void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        event.register(AttackController.TOGGLE_KEY);
    }
}
