package com.shiroha.mmdskin.ui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shiroha.mmdskin.config.PathConstants;
import com.shiroha.mmdskin.config.UIConstants;
import com.shiroha.mmdskin.ui.network.PlayerModelSyncManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 模型选择配置管理
 * 保存每个玩家选择的模型
 * 
 * 重构说明：
 * - 使用 ConcurrentHashMap 保证线程安全
 * - 添加异常处理和重试机制
 * - 改进日志记录
 * - 添加配置验证
 */
public class ModelSelectorConfig {
    private static final Logger logger = LogManager.getLogger();
    private static ModelSelectorConfig instance;
    
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private ConfigData data;
    private long lastSaveTime = 0;
    private static final long SAVE_COOLDOWN = 1000; // 保存冷却时间 1 秒

    private ModelSelectorConfig() {
        load();
    }

    public static synchronized ModelSelectorConfig getInstance() {
        if (instance == null) {
            instance = new ModelSelectorConfig();
        }
        return instance;
    }

    /**
     * 加载配置（带重试机制）
     */
    private void load() {
        File configFile = PathConstants.getModelSelectorConfigFile();
        
        if (configFile.exists()) {
            int retryCount = 0;
            int maxRetries = 3;
            
            while (retryCount < maxRetries) {
                try (Reader reader = new InputStreamReader(new FileInputStream(configFile), java.nio.charset.StandardCharsets.UTF_8)) {
                    data = gson.fromJson(reader, ConfigData.class);
                    
                    // 验证数据
                    if (data == null || data.playerModels == null) {
                        throw new IOException("配置数据无效");
                    }
                    
                    logger.info("模型选择配置加载成功 ({} 个玩家)", data.playerModels.size());
                    return;
                } catch (Exception e) {
                    retryCount++;
                    logger.warn("加载模型选择配置失败 (尝试 {}/{}): {}", retryCount, maxRetries, e.getMessage());
                    
                    if (retryCount >= maxRetries) {
                        logger.error("配置加载失败，使用默认配置", e);
                        data = new ConfigData();
                        saveInternal(true);
                    }
                }
            }
        } else {
            data = new ConfigData();
            saveInternal(true);
        }
    }

    /**
     * 保存配置（带冷却和异常处理）
     */
    public synchronized void save() {
        saveInternal(false);
    }
    
    /**
     * 内部保存实现
     * @param force 是否强制保存（跳过冷却检查）
     */
    private void saveInternal(boolean force) {
        // 冷却检查，避免频繁保存
        long currentTime = System.currentTimeMillis();
        if (!force && currentTime - lastSaveTime < SAVE_COOLDOWN) {
            return;
        }
        
        File configFile = PathConstants.getModelSelectorConfigFile();
        
        // 确保目录存在
        PathConstants.ensureDirectoryExists(configFile.getParentFile());
        
        int retryCount = 0;
        int maxRetries = 3;
        
        while (retryCount < maxRetries) {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), java.nio.charset.StandardCharsets.UTF_8)) {
                gson.toJson(data, writer);
                lastSaveTime = currentTime;
                logger.debug("模型选择配置保存成功");
                return;
            } catch (Exception e) {
                retryCount++;
                logger.warn("保存模型选择配置失败 (尝试 {}/{}): {}", retryCount, maxRetries, e.getMessage());
                
                if (retryCount >= maxRetries) {
                    logger.error("配置保存失败", e);
                }
            }
        }
    }

    /**
     * 获取当前玩家选择的模型
     */
    public String getSelectedModel() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            return getPlayerModel(mc.player.getName().getString());
        }
        return UIConstants.DEFAULT_MODEL_NAME;
    }
    
    /**
     * 获取指定玩家选择的模型
     */
    public String getPlayerModel(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return UIConstants.DEFAULT_MODEL_NAME;
        }
        return data.playerModels.getOrDefault(playerName, UIConstants.DEFAULT_MODEL_NAME);
    }
    
    /**
     * 设置当前玩家选择的模型
     */
    public void setSelectedModel(String modelName) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null) {
            setPlayerModel(mc.player.getName().getString(), modelName);
        }
    }
    
    /**
     * 设置指定玩家选择的模型
     */
    public void setPlayerModel(String playerName, String modelName) {
        if (playerName == null || playerName.isEmpty()) {
            logger.warn("尝试为空玩家名设置模型");
            return;
        }
        
        if (modelName == null) {
            modelName = UIConstants.DEFAULT_MODEL_NAME;
        }
        
        data.playerModels.put(playerName, modelName);
        save();
        logger.info("玩家 {} 选择模型: {}", playerName, modelName);
        
        // 广播到服务器（联机同步）
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player.getName().getString().equals(playerName)) {
            PlayerModelSyncManager.broadcastLocalModelSelection(mc.player.getUUID(), modelName);
        }
    }
    
    /**
     * 移除玩家的模型选择
     */
    public void removePlayerModel(String playerName) {
        if (data.playerModels.remove(playerName) != null) {
            save();
            logger.info("已移除玩家 {} 的模型选择", playerName);
        }
    }
    
    /**
     * 获取所有玩家的模型选择
     */
    public Map<String, String> getAllPlayerModels() {
        return new ConcurrentHashMap<>(data.playerModels);
    }
    
    /**
     * 配置数据类（线程安全）
     */
    private static class ConfigData {
        // 使用 ConcurrentHashMap 保证线程安全
        Map<String, String> playerModels = new ConcurrentHashMap<>();
    }
}
