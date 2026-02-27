package com.shiroha.mmdskin.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型专属动画映射配置
 * 
 * 管理模型目录下的 animations.json，将动画槽位名映射到具体的 VMD 文件名。
 * 例如：{"idle": "my_idle.vmd", "walk": "走路_v2.vmd"}
 * 
 * 映射的 VMD 文件名相对于模型的 anims/ 子文件夹。
 * 
 * 线程安全：使用 ConcurrentHashMap 缓存
 */
public final class ModelAnimConfig {
    private static final Logger logger = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    
    /** 内存缓存：模型目录路径 -> 动画映射 */
    private static final ConcurrentHashMap<String, Map<String, String>> cache = new ConcurrentHashMap<>();
    
    private ModelAnimConfig() {}
    
    /**
     * 获取指定模型的动画映射（带缓存）
     * 
     * @param modelDir 模型目录路径
     * @return 动画槽位 -> VMD 文件名 的映射，不可变视图
     */
    public static Map<String, String> getMapping(String modelDir) {
        if (modelDir == null || modelDir.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(
            cache.computeIfAbsent(modelDir, ModelAnimConfig::loadFromDisk)
        );
    }
    
    /**
     * 获取指定槽位的映射文件名
     * 
     * @param modelDir 模型目录路径
     * @param slotName 动画槽位名（如 "idle"、"walk"）
     * @return VMD 文件名（不含路径），未配置则返回 null
     */
    public static String getMappedFile(String modelDir, String slotName) {
        Map<String, String> mapping = getMapping(modelDir);
        return mapping.get(slotName);
    }
    
    /**
     * 设置动画映射并保存
     * 
     * @param modelDir 模型目录路径
     * @param slotName 动画槽位名
     * @param vmdFileName VMD 文件名（null 表示清除该映射）
     */
    public static void setMapping(String modelDir, String slotName, String vmdFileName) {
        Map<String, String> mapping = cache.computeIfAbsent(modelDir, ModelAnimConfig::loadFromDisk);
        
        if (vmdFileName == null || vmdFileName.isEmpty()) {
            mapping.remove(slotName);
        } else {
            mapping.put(slotName, vmdFileName);
        }
        
        saveToDisk(modelDir, mapping);
    }
    
    /**
     * 批量保存动画映射
     * 
     * @param modelDir 模型目录路径
     * @param mapping 完整的动画映射（会覆盖现有配置）
     */
    public static void saveMapping(String modelDir, Map<String, String> mapping) {
        ConcurrentHashMap<String, String> safeCopy = new ConcurrentHashMap<>(mapping);
        // 清除空值
        safeCopy.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
        cache.put(modelDir, safeCopy);
        saveToDisk(modelDir, safeCopy);
    }
    
    /**
     * 使指定模型的缓存失效
     */
    public static void invalidate(String modelDir) {
        cache.remove(modelDir);
    }
    
    /**
     * 清除所有缓存
     */
    public static void invalidateAll() {
        cache.clear();
    }
    
    // ==================== 磁盘 IO ====================
    
    private static ConcurrentHashMap<String, String> loadFromDisk(String modelDir) {
        File configFile = PathConstants.getModelAnimConfigFile(modelDir);
        if (!configFile.exists()) {
            return new ConcurrentHashMap<>();
        }
        
        try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            Map<String, String> raw = GSON.fromJson(reader, MAP_TYPE);
            if (raw == null) {
                return new ConcurrentHashMap<>();
            }
            ConcurrentHashMap<String, String> result = new ConcurrentHashMap<>();
            // 过滤无效条目
            raw.forEach((k, v) -> {
                if (k != null && !k.isEmpty() && v != null && !v.isEmpty()) {
                    result.put(k, v);
                }
            });
            return result;
        } catch (Exception e) {
            logger.warn("加载模型动画映射失败: {}", e.getMessage());
            return new ConcurrentHashMap<>();
        }
    }
    
    private static void saveToDisk(String modelDir, Map<String, String> mapping) {
        File configFile = PathConstants.getModelAnimConfigFile(modelDir);
        PathConstants.ensureDirectoryExists(configFile.getParentFile());
        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            GSON.toJson(mapping, writer);
            logger.debug("保存模型动画映射: {} ({} 条)", configFile.getName(), mapping.size());
        } catch (IOException e) {
            logger.error("保存模型动画映射失败: {}", e.getMessage());
        }
    }
}
