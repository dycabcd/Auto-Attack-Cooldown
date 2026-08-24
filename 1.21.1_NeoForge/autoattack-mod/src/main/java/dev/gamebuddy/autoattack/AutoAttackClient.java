package dev.gamebuddy.autoattack;

import dev.gamebuddy.autoattack.config.AutoAttackConfigScreenFactory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AutoAttackCooldown — NeoForge 1.21.1 客户端模组入口。
 * <p>
 * 与 Forge 1.20.1 版结构一致：ModConfigSpec 驱动配置、J 键切换开关、
 * 每客户端 tick 调度 AttackController。仅映射与事件包名从
 * {@code net.minecraftforge} 迁移到 {@code net.neoforged}。
 */
@Mod(AutoAttackClient.MOD_ID)
@OnlyIn(Dist.CLIENT)
public class AutoAttackClient {

    public static final String MOD_ID = "autoattack";
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoAttack");

    public AutoAttackClient(IEventBus modBus) {
        // ★ NeoForge 1.21.1 配置与扩展点均通过 ModContainer 注册
        // （ModLoadingContext 不再提供 registerConfig）
        ModContainer container = ModLoadingContext.get().getActiveContainer();
        container.registerConfig(
                ModConfig.Type.CLIENT,
                AutoAttackConfigSpec.SPEC,
                "autoattack.toml"
        );
        // ★ 程序化注册配置界面：通过 ModContainer.registerExtensionPoint
        // （而非 mods.toml 的 configScreen 字段）。显式 cast 为 Supplier 以消除重载歧义
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (java.util.function.Supplier<IConfigScreenFactory>) () -> new AutoAttackConfigScreenFactory()
        );

        // ★ 在构造函数中手动注册配置事件监听，确保注册时机早于 ModConfigEvent 的触发
        modBus.addListener(AutoAttackConfig::onModConfigEvent);

        // 按键注册
        modBus.addListener(this::onRegisterKeyMappings);

        // ★ 将每客户端 tick 回调注册到 NeoForge 事件总线
        // ClientTickEvent.Post 对应 1.20.1 的 TickEvent.ClientTickEvent + END phase
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> AttackController.onClientTick());

        LOGGER.info("[AutoAttack] 已加载，当前状态：{}", AutoAttackConfig.enabled ? "开启" : "关闭");
    }

    private void onRegisterKeyMappings(final RegisterKeyMappingsEvent event) {
        event.register(AttackController.TOGGLE_KEY);
    }
}
