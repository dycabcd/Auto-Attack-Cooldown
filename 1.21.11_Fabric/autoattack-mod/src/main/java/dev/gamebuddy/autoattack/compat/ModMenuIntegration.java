package dev.gamebuddy.autoattack.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.gamebuddy.autoattack.config.AutoAttackConfigScreen;

/**
 * ModMenu 集成入口。
 * 在模组菜单中点击"配置"时打开 Cloth Config 设置界面。
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AutoAttackConfigScreen::create;
    }
}