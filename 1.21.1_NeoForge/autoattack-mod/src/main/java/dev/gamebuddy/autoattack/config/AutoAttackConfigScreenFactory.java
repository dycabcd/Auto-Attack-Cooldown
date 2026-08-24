package dev.gamebuddy.autoattack.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.minecraft.client.gui.screens.Screen;

/**
 * 配置界面工厂。
 * <p>
 * 由 {@link dev.gamebuddy.autoattack.AutoAttackClient} 通过
 * {@code ModContainer.registerExtensionPoint} 注册，在模组列表点击
 * "配置"按钮时创建 {@link AutoAttackConfigScreen}。
 */
@OnlyIn(Dist.CLIENT)
public class AutoAttackConfigScreenFactory implements IConfigScreenFactory {

    @Override
    public Screen createScreen(ModContainer container, Screen parent) {
        return new AutoAttackConfigScreen(parent);
    }
}
