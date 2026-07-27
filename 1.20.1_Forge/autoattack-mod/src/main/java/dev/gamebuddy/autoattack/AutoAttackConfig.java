package dev.gamebuddy.autoattack;

import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 自动攻击冷却 — 配置管理。
 * <p>
 * 纯 ForgeConfigSpec 驱动，所有配置存储在 {@code autoattack.toml}。
 * 运行时缓存通过 {@link #bake()} 从 ForgeConfigSpec 同步。
 * {@code enabled} 字段由游戏内 J 键切换时同步更新 ForgeConfigSpec 并通过
 * {@link #modConfig} 持久化。
 * <p>
 * 注意：此类不标记 @Mod.EventBusSubscriber，事件注册由
 * {@link AutoAttackClient} 在构造函数中手动完成，确保在配置加载前注册。
 */
public class AutoAttackConfig {

    // ====== 运行时缓存 ======
    public static boolean enabled = true;
    public static double cooldownThreshold = 1.0;
    public static boolean onlyAttackEntities = false;
    public static boolean preventBlockBreaking = true;
    public static boolean respectUsingItem = true;
    public static boolean showToggleMessage = true;

    // ====== 穿草攻击缓存 ======
    public static boolean penetrationEnabled = true;
    public static double penetrationAngle = 20.0;
    public static double penetrationMargin = 2.0;
    public static boolean penetrationDamageOnly = true;

    /** 保存引用，用于运行时 toggle 和配置界面持久化 */
    public static ModConfig modConfig;

    private AutoAttackConfig() {
    }

    /**
     * 由 {@link AutoAttackClient} 注册的 ModConfigEvent 回调。
     * 同时处理 Loading 和 Reloading。
     */
    public static void onModConfigEvent(final ModConfigEvent event) {
        if (event.getConfig().getSpec() == AutoAttackConfigSpec.SPEC) {
            modConfig = event.getConfig();
            bake();
        }
    }

    /**
     * 将 ForgeConfigSpec 值同步到运行时缓存
     */
    public static void bake() {
        enabled = AutoAttackConfigSpec.ENABLED.get();
        cooldownThreshold = AutoAttackConfigSpec.COOLDOWN_THRESHOLD.get();
        onlyAttackEntities = AutoAttackConfigSpec.ONLY_ATTACK_ENTITIES.get();
        preventBlockBreaking = AutoAttackConfigSpec.PREVENT_BLOCK_BREAKING.get();
        respectUsingItem = AutoAttackConfigSpec.RESPECT_USING_ITEM.get();
        showToggleMessage = AutoAttackConfigSpec.SHOW_TOGGLE_MESSAGE.get();

        penetrationEnabled = AutoAttackConfigSpec.PENETRATION_ENABLED.get();
        penetrationAngle = AutoAttackConfigSpec.PENETRATION_ANGLE.get();
        penetrationMargin = AutoAttackConfigSpec.PENETRATION_MARGIN.get();
        penetrationDamageOnly = AutoAttackConfigSpec.PENETRATION_DAMAGE_ONLY.get();
    }

    /**
     * 切换 enabled 状态并持久化到配置文件
     */
    public static void toggle() {
        enabled = !enabled;
        AutoAttackConfigSpec.ENABLED.set(enabled);
        if (modConfig != null) {
            modConfig.save();
        }
    }
}
