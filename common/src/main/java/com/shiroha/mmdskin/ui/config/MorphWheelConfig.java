package com.shiroha.mmdskin.ui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.shiroha.mmdskin.config.PathConstants;
import com.shiroha.mmdskin.renderer.animation.MorphInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 表情轮盘配置管理
 * 管理可用的表情预设和轮盘显示的表情
 */
public class MorphWheelConfig {
    private static final Logger logger = LogManager.getLogger();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private static MorphWheelConfig instance;
    
    private List<MorphEntry> availableMorphs = new ArrayList<>();
    private List<MorphEntry> displayedMorphs = new ArrayList<>();
    
    /**
     * 表情条目
     */
    public static class MorphEntry {
        public String displayName;
        public String morphName;
        public String source;
        public String modelName;
        public String fileSize;
        public boolean selected;
        
        public MorphEntry() {}
        
        public MorphEntry(String displayName, String morphName, String source, 
                          String modelName, String fileSize) {
            this.displayName = displayName;
            this.morphName = morphName;
            this.source = source;
            this.modelName = modelName;
            this.fileSize = fileSize;
            this.selected = false;
        }
    }
    
    public static synchronized MorphWheelConfig getInstance() {
        if (instance == null) {
            instance = new MorphWheelConfig();
            instance.load();
        }
        return instance;
    }
    
    /**
     * 扫描可用表情
     */
    public void scanAvailableMorphs() {
        availableMorphs.clear();
        
        List<MorphInfo> morphs = MorphInfo.scanAllMorphs();
        
        for (MorphInfo morph : morphs) {
            availableMorphs.add(new MorphEntry(
                morph.getDisplayName(),
                morph.getMorphName(),
                morph.getSource().name(),
                morph.getModelName(),
                morph.getFormattedSize()
            ));
        }
        
        // 恢复之前的选择状态
        for (MorphEntry available : availableMorphs) {
            for (MorphEntry displayed : displayedMorphs) {
                if (available.morphName.equals(displayed.morphName)) {
                    available.selected = true;
                    break;
                }
            }
        }
        
        logger.info("扫描到 {} 个可用表情", availableMorphs.size());
    }
    
    /**
     * 获取可用表情列表
     */
    public List<MorphEntry> getAvailableMorphs() {
        return availableMorphs;
    }
    
    /**
     * 获取显示的表情列表（返回不可变视图）
     */
    public List<MorphEntry> getDisplayedMorphs() {
        return java.util.Collections.unmodifiableList(displayedMorphs);
    }
    
    /**
     * 设置显示的表情列表
     */
    public void setDisplayedMorphs(List<MorphEntry> morphs) {
        this.displayedMorphs = new ArrayList<>(morphs);
    }
    
    /**
     * 更新显示的表情（基于选择状态）
     */
    public void updateDisplayedMorphs() {
        displayedMorphs.clear();
        for (MorphEntry entry : availableMorphs) {
            if (entry.selected) {
                displayedMorphs.add(entry);
            }
        }
    }
    
    /**
     * 选择所有表情
     */
    public void selectAll() {
        for (MorphEntry entry : availableMorphs) {
            entry.selected = true;
        }
        updateDisplayedMorphs();
    }
    
    /**
     * 清除所有选择
     */
    public void clearAll() {
        for (MorphEntry entry : availableMorphs) {
            entry.selected = false;
        }
        updateDisplayedMorphs();
    }
    
    /**
     * 切换表情选择状态
     */
    public void toggleMorph(String morphName) {
        for (MorphEntry entry : availableMorphs) {
            if (entry.morphName.equals(morphName)) {
                entry.selected = !entry.selected;
                break;
            }
        }
        updateDisplayedMorphs();
    }
    
    /**
     * 加载配置
     */
    public void load() {
        File configFile = PathConstants.getMorphWheelConfigFile();
        
        if (!configFile.exists()) {
            // 首次使用，扫描并全选
            scanAvailableMorphs();
            selectAll();
            save();
            return;
        }
        
        try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<MorphEntry>>(){}.getType();
            List<MorphEntry> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                displayedMorphs = loaded;
            }
            logger.info("加载表情轮盘配置: {} 个表情", displayedMorphs.size());
        } catch (Exception e) {
            logger.error("加载表情配置失败", e);
        }
        
        // 扫描可用表情
        scanAvailableMorphs();
    }
    
    /**
     * 保存配置
     */
    public void save() {
        File configFile = PathConstants.getMorphWheelConfigFile();
        
        // 确保父目录存在
        PathConstants.ensureDirectoryExists(configFile.getParentFile());
        
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
            gson.toJson(displayedMorphs, writer);
            logger.info("保存表情轮盘配置: {} 个表情", displayedMorphs.size());
        } catch (Exception e) {
            logger.error("保存表情配置失败", e);
        }
    }
    
    /**
     * 重新加载（就地刷新当前实例数据）
     */
    public void reload() {
        load();
    }
}
