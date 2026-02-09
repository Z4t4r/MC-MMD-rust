package com.shiroha.mmdskin.ui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.shiroha.mmdskin.config.PathConstants;
import com.shiroha.mmdskin.renderer.animation.AnimationInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 动作轮盘配置管理
 * 负责加载和保存动作配置，扫描可用动作文件
 * 
 * 重构说明：
 * - 使用 AnimationInfo 扫描所有目录的 VMD 文件
 * - 支持 DefaultAnim、CustomAnim 和模型专属动画
 */
public class ActionWheelConfig {
    private static final Logger LOGGER = LogManager.getLogger();
    private static ActionWheelConfig instance;
    
    private List<ActionEntry> displayedActions; // 轮盘显示的动作
    private List<ActionEntry> availableActions; // 所有可用的动作

    private ActionWheelConfig() {
        this.displayedActions = new ArrayList<>();
        this.availableActions = new ArrayList<>();
        scanAvailableActions();
    }

    public static synchronized ActionWheelConfig getInstance() {
        if (instance == null) {
            instance = new ActionWheelConfig();
            instance.load(); // 只在首次创建时加载
        }
        return instance;
    }
    
    /**
     * 重置实例（用于测试或强制重新加载）
     */
    public static synchronized void reset() {
        instance = null;
    }

    /**
     * 扫描所有动画目录获取可用动作
     * 使用 AnimationInfo 扫描：DefaultAnim、CustomAnim、模型目录
     */
    public void scanAvailableActions() {
        availableActions.clear();
        
        // 使用 AnimationInfo 扫描所有动画
        List<AnimationInfo> animations = AnimationInfo.scanAllAnimations();
        
        for (AnimationInfo anim : animations) {
            availableActions.add(new ActionEntry(
                anim.getDisplayName(),
                anim.getAnimName(),
                anim.getSource().name(),
                anim.getModelName(),
                anim.getFormattedSize()
            ));
        }
        
        LOGGER.info("共扫描到 {} 个动画", availableActions.size());
    }

    /**
     * 从文件加载配置
     * 使用 UTF-8 编码支持中文
     */
    public void load() {
        try {
            File configFile = PathConstants.getActionWheelConfigFile();
            if (!configFile.exists()) {
                LOGGER.info("动作轮盘配置文件不存在，使用默认配置");
                loadDefaultDisplayedActions();
                save();
                return;
            }

            try (Reader reader = new InputStreamReader(new FileInputStream(configFile), java.nio.charset.StandardCharsets.UTF_8)) {
                Gson gson = new Gson();
                ConfigData data = gson.fromJson(reader, ConfigData.class);
                if (data != null && data.displayedActions != null) {
                    this.displayedActions = data.displayedActions;
                    // 过滤掉不存在的动作
                    filterValidActions();
                    LOGGER.info("成功加载动作轮盘配置，显示 {} 个动作", displayedActions.size());
                } else {
                    loadDefaultDisplayedActions();
                }
            }
        } catch (Exception e) {
            LOGGER.error("加载动作轮盘配置失败", e);
            loadDefaultDisplayedActions();
        }
    }

    /**
     * 加载默认显示的动作（所有可用动作）
     */
    private void loadDefaultDisplayedActions() {
        displayedActions.clear();
        displayedActions.addAll(availableActions);
        LOGGER.info("加载默认动作配置，共 {} 个动作", displayedActions.size());
    }

    /**
     * 过滤掉不存在的动作
     */
    private void filterValidActions() {
        displayedActions.removeIf(displayed -> 
            availableActions.stream().noneMatch(available -> 
                available.animId.equals(displayed.animId)
            )
        );
    }

    /**
     * 保存配置到文件
     * 使用 UTF-8 编码支持中文
     */
    public void save() {
        try {
            File configFile = PathConstants.getActionWheelConfigFile();
            PathConstants.ensureDirectoryExists(configFile.getParentFile());

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), java.nio.charset.StandardCharsets.UTF_8)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                ConfigData data = new ConfigData();
                data.displayedActions = this.displayedActions;
                gson.toJson(data, writer);
                LOGGER.info("动作轮盘配置已保存");
            }
        } catch (Exception e) {
            LOGGER.error("保存动作轮盘配置失败", e);
        }
    }


    /**
     * 获取轮盘显示的动作列表
     */
    public List<ActionEntry> getDisplayedActions() {
        return Collections.unmodifiableList(displayedActions);
    }

    /**
     * 获取所有可用的动作列表
     */
    public List<ActionEntry> getAvailableActions() {
        return Collections.unmodifiableList(availableActions);
    }

    /**
     * 设置轮盘显示的动作
     */
    public void setDisplayedActions(List<ActionEntry> actions) {
        this.displayedActions = actions;
    }

    /**
     * 重新扫描动作文件
     */
    public void rescan() {
        scanAvailableActions();
        filterValidActions();
    }

    /**
     * 动作条目
     */
    public static class ActionEntry {
        public String name;      // 显示名称
        public String animId;    // 动画ID（文件名，不含.vmd）
        public String source;    // 来源：DEFAULT, CUSTOM, MODEL
        public String modelName; // 模型名称（仅模型专属动画有效）
        public String fileSize;  // 格式化的文件大小

        public ActionEntry() {}

        public ActionEntry(String name, String animId) {
            this.name = name;
            this.animId = animId;
            this.source = "CUSTOM";
            this.modelName = null;
            this.fileSize = "";
        }
        
        public ActionEntry(String name, String animId, String source, String modelName, String fileSize) {
            this.name = name;
            this.animId = animId;
            this.source = source;
            this.modelName = modelName;
            this.fileSize = fileSize;
        }
        
        /**
         * 获取来源描述
         */
        public String getSourceDescription() {
            if (source == null) return "未知";
            switch (source) {
                case "DEFAULT": return "默认动画";
                case "CUSTOM": return "自定义动画";
                case "MODEL": return modelName != null ? "模型专属 (" + modelName + ")" : "模型专属";
                default: return source;
            }
        }
    }

    /**
     * 配置数据容器
     */
    private static class ConfigData {
        List<ActionEntry> displayedActions;
    }
}
