package com.shiroha.mmdskin.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型独立配置管理器
 * 管理每个模型的独立配置（眼球角度、物理等），
 * 配置文件存储在 config/mmdskin/model_configs/<模型名>.json
 */
public class ModelConfigManager {
    private static final Logger logger = LogManager.getLogger();
    
    /** 模型配置目录名 */
    private static final String MODEL_CONFIGS_DIR = "model_configs";
    
    /** 内存缓存：模型名 -> 配置数据 */
    private static final ConcurrentHashMap<String, ModelConfigData> cache = new ConcurrentHashMap<>();
    
    private ModelConfigManager() {
        // 工具类，禁止实例化
    }
    
    /**
     * 获取指定模型的配置（带缓存）
     * @param modelName 模型文件夹名称
     * @return 模型配置，不存在则返回默认值
     */
    public static ModelConfigData getConfig(String modelName) {
        if (modelName == null || modelName.isEmpty() 
                || modelName.equals(UIConstants.DEFAULT_MODEL_NAME)) {
            return new ModelConfigData();
        }
        
        return cache.computeIfAbsent(modelName, name -> {
            File configFile = getConfigFile(name);
            ModelConfigData config = ModelConfigData.load(configFile);
            logger.debug("加载模型配置: {} (眼球角度: {})", name, config.eyeMaxAngle);
            return config;
        });
    }
    
    /**
     * 保存指定模型的配置
     * @param modelName 模型文件夹名称
     * @param config 配置数据
     */
    public static void saveConfig(String modelName, ModelConfigData config) {
        if (modelName == null || modelName.isEmpty() 
                || modelName.equals(UIConstants.DEFAULT_MODEL_NAME)) {
            return;
        }
        
        ModelConfigData safeCopy = config.copy();
        cache.put(modelName, safeCopy);
        File configFile = getConfigFile(modelName);
        safeCopy.save(configFile);
        logger.info("保存模型配置: {} (眼球角度: {})", modelName, config.eyeMaxAngle);
    }
    
    /**
     * 清除缓存中指定模型的配置（下次访问时重新加载）
     */
    public static void invalidate(String modelName) {
        cache.remove(modelName);
    }
    
    /**
     * 清除所有缓存
     */
    public static void invalidateAll() {
        cache.clear();
    }
    
    /**
     * 获取模型配置文件路径
     */
    public static File getConfigFile(String modelName) {
        File dir = new File(PathConstants.getConfigRootDir(), MODEL_CONFIGS_DIR);
        return new File(dir, sanitizeModelName(modelName) + ".json");
    }
    
    /**
     * 过滤模型名称中的危险字符，防止路径穿越
     */
    private static String sanitizeModelName(String name) {
        if (name == null) return "unknown";
        return name.replace('/', '_').replace('\\', '_')
                   .replace("..", "__").replace('\0', '_');
    }
    
    /**
     * 获取模型配置目录
     */
    public static File getConfigDir() {
        return new File(PathConstants.getConfigRootDir(), MODEL_CONFIGS_DIR);
    }
}
