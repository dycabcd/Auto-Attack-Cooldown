package dev.gamebuddy.autoattack.config;

import dev.gamebuddy.autoattack.AutoAttackConfig;
import dev.gamebuddy.autoattack.AutoAttackConfigSpec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 自定义 NeoForge 配置界面（双列紧凑布局）。
 * <p>
 * 与 Forge 1.20.1 版界面一致。由 {@link AutoAttackConfigScreenFactory}
 * 通过 {@code ModContainer.registerExtensionPoint} 程序化创建
 *（而非 mods.toml 的 configScreen 字段）。
 */
@OnlyIn(Dist.CLIENT)
public class AutoAttackConfigScreen extends Screen {
    private final Screen parent;

    private static final int COL_WIDTH = 180;
    private static final int ROW_H = 20;
    private static final int W_H = 18;
    private static final int GAP = 16;
    private static final int TOP_Y = 28;

    protected AutoAttackConfigScreen(Screen parent) {
        super(Component.translatable("title.autoattack.config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int totalW = COL_WIDTH * 2 + GAP;
        int leftX = (this.width - totalW) / 2;
        int rightX = leftX + COL_WIDTH + GAP;
        int ly = TOP_Y;
        int ry = TOP_Y;

        // ═══════════ 左列：通用选项 ═══════════
        addToggle(leftX, ly, "option.autoattack.enabled",
                () -> AutoAttackConfig.enabled,
                v -> setEnabled(v));
        ly += ROW_H;

        addToggle(leftX, ly, "option.autoattack.onlyAttackEntities",
                () -> AutoAttackConfig.onlyAttackEntities,
                v -> setOnlyAttackEntities(v));
        ly += ROW_H;

        addToggle(leftX, ly, "option.autoattack.preventBlockBreaking",
                () -> AutoAttackConfig.preventBlockBreaking,
                v -> setPreventBlockBreaking(v));
        ly += ROW_H;

        this.addRenderableWidget(new PercentSlider(
                leftX, ly, COL_WIDTH, W_H,
                AutoAttackConfig.cooldownThreshold));
        ly += ROW_H;

        addToggle(leftX, ly, "option.autoattack.respectUsingItem",
                () -> AutoAttackConfig.respectUsingItem,
                v -> setRespectUsingItem(v));
        ly += ROW_H;

        addToggle(leftX, ly, "option.autoattack.showToggleMessage",
                () -> AutoAttackConfig.showToggleMessage,
                v -> setShowToggleMessage(v));

        // ═══════════ 右列：穿草攻击 ═══════════
        this.addRenderableWidget(Button.builder(
                Component.literal("§l穿草攻击"), btn -> {})
                .pos(rightX, ry).size(COL_WIDTH, W_H)
                .build());
        ry += ROW_H;

        addToggle(rightX, ry, "option.autoattack.penetrationEnabled",
                () -> AutoAttackConfig.penetrationEnabled,
                v -> setPenetrationEnabled(v));
        ry += ROW_H;

        this.addRenderableWidget(new AngleSlider(
                rightX, ry, COL_WIDTH, W_H));
        ry += ROW_H;

        this.addRenderableWidget(new MarginSlider(
                rightX, ry, COL_WIDTH, W_H));
        ry += ROW_H;

        addToggle(rightX, ry, "option.autoattack.penetrationDamageOnly",
                () -> AutoAttackConfig.penetrationDamageOnly,
                v -> setPenetrationDamageOnly(v));

        // ═══════════ 完成按钮 ═══════════
        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                btn -> Minecraft.getInstance().setScreen(this.parent))
                .pos(leftX, this.height - 26)
                .size(totalW, 20)
                .build());
    }

    // ──────── 切换按钮 ────────

    private void addToggle(int x, int y, String key,
                           Supplier<Boolean> getter,
                           Consumer<Boolean> setter) {
        String label = Component.translatable(key).getString();
        this.addRenderableWidget(Button.builder(
                toggleText(label, getter.get()),
                btn -> {
                    boolean nv = !getter.get(); // ★ 实时读当前值
                    setter.accept(nv);
                    btn.setMessage(toggleText(label, nv));
                })
                .pos(x, y).size(COL_WIDTH, W_H)
                .build());
    }

    private static Component toggleText(String label, boolean on) {
        return Component.literal(label + (on ? " §a●" : " §c○"));
    }

    // ──────── 各配置项 setter（只改缓存和 spec，不写磁盘 —— 屏幕关闭时统一保存）───────

    private static void setEnabled(boolean v) { AutoAttackConfig.enabled = v; AutoAttackConfigSpec.ENABLED.set(v); }
    private static void setOnlyAttackEntities(boolean v) { AutoAttackConfig.onlyAttackEntities = v; AutoAttackConfigSpec.ONLY_ATTACK_ENTITIES.set(v); }
    private static void setPreventBlockBreaking(boolean v) { AutoAttackConfig.preventBlockBreaking = v; AutoAttackConfigSpec.PREVENT_BLOCK_BREAKING.set(v); }
    private static void setRespectUsingItem(boolean v) { AutoAttackConfig.respectUsingItem = v; AutoAttackConfigSpec.RESPECT_USING_ITEM.set(v); }
    private static void setShowToggleMessage(boolean v) { AutoAttackConfig.showToggleMessage = v; AutoAttackConfigSpec.SHOW_TOGGLE_MESSAGE.set(v); }
    private static void setPenetrationEnabled(boolean v) { AutoAttackConfig.penetrationEnabled = v; AutoAttackConfigSpec.PENETRATION_ENABLED.set(v); }
    private static void setPenetrationDamageOnly(boolean v) { AutoAttackConfig.penetrationDamageOnly = v; AutoAttackConfigSpec.PENETRATION_DAMAGE_ONLY.set(v); }

    // ──────── 滑块 ────────

    private class PercentSlider extends AbstractSliderButton {
        PercentSlider(int x, int y, int w, int h, double initial) {
            super(x, y, w, h,
                    Component.literal("冷却阈值 " + pctText(initial)), initial);
        }
        @Override protected void updateMessage() { this.setMessage(Component.literal("冷却阈值 " + pctText(this.value))); }
        @Override protected void applyValue() { AutoAttackConfig.cooldownThreshold = this.value; AutoAttackConfigSpec.COOLDOWN_THRESHOLD.set(this.value); }
        private static String pctText(double v) { return (int) Math.round(v * 100) + "%"; }
    }

    private class AngleSlider extends AbstractSliderButton {
        AngleSlider(int x, int y, int w, int h) {
            super(x, y, w, h,
                    Component.literal("穿透角度 " + angleText(AutoAttackConfig.penetrationAngle)),
                    (AutoAttackConfig.penetrationAngle - 5.0) / (45.0 - 5.0));
        }
        @Override protected void updateMessage() { double a = 5.0 + this.value * (45.0 - 5.0); this.setMessage(Component.literal("穿透角度 " + angleText(a))); }
        @Override protected void applyValue() { double a = 5.0 + this.value * (45.0 - 5.0); AutoAttackConfig.penetrationAngle = a; AutoAttackConfigSpec.PENETRATION_ANGLE.set(a); }
        private static String angleText(double a) { return (int) a + "°"; }
    }

    private class MarginSlider extends AbstractSliderButton {
        MarginSlider(int x, int y, int w, int h) {
            super(x, y, w, h,
                    Component.literal("搜索余量 " + marginText(AutoAttackConfig.penetrationMargin)),
                    (AutoAttackConfig.penetrationMargin - 0.5) / (5.0 - 0.5));
        }
        @Override protected void updateMessage() { double m = 0.5 + this.value * (5.0 - 0.5); this.setMessage(Component.literal("搜索余量 " + marginText(m))); }
        @Override protected void applyValue() { double m = 0.5 + this.value * (5.0 - 0.5); AutoAttackConfig.penetrationMargin = m; AutoAttackConfigSpec.PENETRATION_MARGIN.set(m); }
        private static String marginText(double m) { return String.format("%.1f", m); }
    }

    private static void saveConfig() {
        // ★ NeoForge 1.21.1 的 ModConfig 不再提供 save()，改由 ModConfigSpec 持久化
        AutoAttackConfigSpec.SPEC.save();
    }

    @Override
    public void removed() {
        // 屏幕关闭时统一持久化所有配置更改（避免每个操作都写磁盘）
        saveConfig();
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1.21 的 renderBackground 签名为四参（含鼠标坐标与 partialTick）
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
