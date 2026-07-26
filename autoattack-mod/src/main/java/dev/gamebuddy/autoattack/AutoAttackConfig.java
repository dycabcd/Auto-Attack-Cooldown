package dev.gamebuddy.autoattack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON 配置文件：.minecraft/config/autoattack.json
 *
 * enabled                    模组总开关（游戏内按 J 键切换并自动保存）
 * cooldownThreshold          触发攻击所需的冷却进度，0.2 ~ 1.0
 * onlyAttackEntities         是否仅攻击实体；关闭时对空气也保持挥击节奏
 * preventBlockBreaking       开启时，模组启用状态下阻止所有方块破坏（避免挖矿干扰战斗）
 * respectUsingItem           进食/拉弓/举盾等状态中暂停自动攻击
 * showToggleMessage          开关切换时在快捷栏上方显示提示
 */
public class AutoAttackConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autoattack.json");

    private static AutoAttackConfig instance;

    // ====== 配置字段 ======
    public boolean enabled = true;
    public double cooldownThreshold = 1.0;
    public boolean onlyAttackEntities = true;
    public boolean preventBlockBreaking = true;
    public boolean respectUsingItem = true;
    public boolean showToggleMessage = true;

    // ====== 单例访问 ======
    public static AutoAttackConfig get() {
        if (instance == null) {
            instance = new AutoAttackConfig();
        }
        return instance;
    }

    // ====== 加载/保存 ======
    public static void load() {
        boolean needsSave = false;
        if (Files.exists(FILE)) {
            try (Reader reader = Files.newBufferedReader(FILE)) {
                AutoAttackConfig loaded = GSON.fromJson(reader, AutoAttackConfig.class);
                instance = (loaded != null) ? loaded : new AutoAttackConfig();
            } catch (Exception e) {
                AutoAttackClient.LOGGER.warn("[AutoAttack] 配置文件读取失败，已重置为默认值: {}", e.toString());
                instance = new AutoAttackConfig();
                needsSave = true;
            }
        } else {
            instance = new AutoAttackConfig();
            needsSave = true;
        }
        if (get().sanitize()) {
            needsSave = true;
        }
        if (needsSave) {
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException e) {
            AutoAttackClient.LOGGER.warn("[AutoAttack] 配置文件保存失败: {}", e.toString());
        }
    }

    // ====== 值域检查 ======
    private boolean sanitize() {
        if (Double.isNaN(cooldownThreshold) || cooldownThreshold < 0.2 || cooldownThreshold > 1.0) {
            cooldownThreshold = 1.0;
            return true;
        }
        return false;
    }
}