package com.shiroha.mmdskin.forge.config;

import com.shiroha.mmdskin.config.AbstractMmdSkinConfig;
import com.shiroha.mmdskin.config.ConfigData;
import com.shiroha.mmdskin.config.ConfigManager;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * Forge 配置实现
 * 继承 AbstractMmdSkinConfig，只需提供平台特定的 configPath
 */
public final class MmdSkinConfig extends AbstractMmdSkinConfig {
    private static final Logger logger = LogManager.getLogger();
    private static MmdSkinConfig instance;
    private static Path configPath;

    private MmdSkinConfig() {
        super(ConfigData.load(FMLPaths.CONFIGDIR.get().resolve("MmdSkin")));
        configPath = FMLPaths.CONFIGDIR.get().resolve("MmdSkin");
    }

    public static void init() {
        instance = new MmdSkinConfig();
        ConfigManager.init(instance);
    }

    /** 获取配置数据（供 UI 使用） */
    public static ConfigData getData() {
        return instance.data;
    }

    /** 保存配置 */
    public static void save() {
        instance.data.save(configPath);
    }
}
