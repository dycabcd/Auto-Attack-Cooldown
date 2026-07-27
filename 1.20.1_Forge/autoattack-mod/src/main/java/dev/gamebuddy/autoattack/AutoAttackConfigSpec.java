package dev.gamebuddy.autoattack;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * ForgeConfigSpec 定义。
 * 所有配置项集中管理，Forge 自动生成配置 GUI。
 */
public final class AutoAttackConfigSpec {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // ====== 通用选项 ======
    public static final ForgeConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("模组总开关（游戏内按 J 键切换）")
            .define("enabled", true);

    public static final ForgeConfigSpec.DoubleValue COOLDOWN_THRESHOLD = BUILDER
            .comment("触发攻击所需的冷却进度 (0.2 ~ 1.0)。1.0=满冷却才出手（最大伤害）；越低出手越早"
                    + "但单次伤害越低")
            .defineInRange("cooldownThreshold", 1.0, 0.2, 1.0);

    public static final ForgeConfigSpec.BooleanValue ONLY_ATTACK_ENTITIES = BUILDER
            .comment("为 true 时仅攻击实体；为 false 时对空气也保持挥击节奏以维持攻击手感")
            .define("onlyAttackEntities", false);

    public static final ForgeConfigSpec.BooleanValue PREVENT_BLOCK_BREAKING = BUILDER
            .comment("开启后阻止所有方块破坏，避免战斗中误挖方块干扰")
            .define("preventBlockBreaking", true);

    public static final ForgeConfigSpec.BooleanValue RESPECT_USING_ITEM = BUILDER
            .comment("进食/拉弓/举盾等使用物品时暂停自动攻击")
            .define("respectUsingItem", true);

    public static final ForgeConfigSpec.BooleanValue SHOW_TOGGLE_MESSAGE = BUILDER
            .comment("J 键切换开关时在快捷栏上方显示提示")
            .define("showToggleMessage", true);

    // ====== 穿草攻击选项 ======
    public static final ForgeConfigSpec.BooleanValue PENETRATION_ENABLED = BUILDER
            .comment("启用以实现穿草攻击——当视线被草丛、藤蔓、花等非碰撞方块遮挡时，"
                    + "仍可攻击后方目标")
            .define("penetrationEnabled", true);

    public static final ForgeConfigSpec.DoubleValue PENETRATION_ANGLE = BUILDER
            .comment("穿草攻击第二阶段角度匹配容差（度）。值越大越容易命中，但也越不精确。"
                    + "推荐值：10~30")
            .defineInRange("penetrationAngle", 20.0, 5.0, 45.0);

    public static final ForgeConfigSpec.DoubleValue PENETRATION_MARGIN = BUILDER
            .comment("穿草攻击搜索框扩展余量（格）。值越大搜索范围越广，"
                    + "但增加性能开销。推荐值：1.0~4.0")
            .defineInRange("penetrationMargin", 2.0, 0.5, 5.0);

    public static final ForgeConfigSpec.BooleanValue PENETRATION_DAMAGE_ONLY = BUILDER
            .comment("为 true 时仅对 LivingEntity 进行穿草检测（矿车、船等非生物实体不受影响）")
            .define("penetrationDamageOnly", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private AutoAttackConfigSpec() {
    }
}
