package dev.gamebuddy.autoattack.config;

import dev.gamebuddy.autoattack.AutoAttackConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Cloth Config 驱动的模组配置界面工厂。
 * 由 ModMenu 调用，返回配置界面。
 */
public class AutoAttackConfigScreen {

    /**
     * 构建配置界面。
     * @param parent 上一层界面（ModMenu 主界面）
     * @return 配置界面 Screen
     */
    public static Screen create(Screen parent) {
        AutoAttackConfig config = AutoAttackConfig.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.autoattack.config"))
                .setSavingRunnable(AutoAttackConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory general = builder.getOrCreateCategory(
                Text.translatable("category.autoattack.general"));

        // 启用模组
        general.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("option.autoattack.enabled"),
                        config.enabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> config.enabled = v)
                .build());

        // 仅攻击实体
        general.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("option.autoattack.onlyAttackEntities"),
                        config.onlyAttackEntities)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("option.autoattack.onlyAttackEntities.tooltip"))
                .setSaveConsumer(v -> config.onlyAttackEntities = v)
                .build());

        // 阻止破坏方块
        general.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("option.autoattack.preventBlockBreaking"),
                        config.preventBlockBreaking)
                .setDefaultValue(true)
                .setTooltip(Text.translatable("option.autoattack.preventBlockBreaking.tooltip"))
                .setSaveConsumer(v -> config.preventBlockBreaking = v)
                .build());

        // 冷却阈值（滑块，显示为整数百分比 20%~100%）
        general.addEntry(entryBuilder.startIntSlider(
                        Text.translatable("option.autoattack.cooldownThreshold"),
                        (int) Math.round(config.cooldownThreshold * 100),
                        20, 100)
                .setDefaultValue(100)
                .setTextGetter(v -> Text.literal(v + "%"))
                .setTooltip(Text.translatable("option.autoattack.cooldownThreshold.tooltip"))
                .setSaveConsumer(v -> config.cooldownThreshold = v / 100.0)
                .build());

        // 使用物品时暂停
        general.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("option.autoattack.respectUsingItem"),
                        config.respectUsingItem)
                .setDefaultValue(true)
                .setSaveConsumer(v -> config.respectUsingItem = v)
                .build());

        // 切换提示
        general.addEntry(entryBuilder.startBooleanToggle(
                        Text.translatable("option.autoattack.showToggleMessage"),
                        config.showToggleMessage)
                .setDefaultValue(true)
                .setSaveConsumer(v -> config.showToggleMessage = v)
                .build());

        return builder.build();
    }
}