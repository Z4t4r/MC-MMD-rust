package com.shiroha.mmdskin.fabric;

import com.shiroha.mmdskin.MmdSkinClient;
import com.shiroha.mmdskin.fabric.config.MmdSkinConfig;
import com.shiroha.mmdskin.fabric.maid.MaidCompatMixinPlugin;
import com.shiroha.mmdskin.fabric.register.MmdSkinRegisterClient;
import com.shiroha.mmdskin.renderer.model.MMDModelOpenGL;

import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric 客户端初始化
 */
public class MmdSkinFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MmdSkinClient.logger.info("MMD Skin Fabric 客户端初始化开始...");
        
        // 初始化配置系统
        MmdSkinConfig.init();
        
        // 初始化客户端
        MmdSkinClient.initClient();
        
        // 注册客户端内容
        MmdSkinRegisterClient.Register();
        
        // 配置 MMD Shader
        MMDModelOpenGL.isMMDShaderEnabled = com.shiroha.mmdskin.config.ConfigManager.isMMDShaderEnabled();
        
        // 女仆模组兼容状态
        if (MaidCompatMixinPlugin.isMaidModLoaded()) {
            MmdSkinClient.logger.info("检测到 TouhouLittleMaid (Orihime) 模组，已启用女仆 MMD 渲染支持");
        } else {
            MmdSkinClient.logger.info("未检测到 TouhouLittleMaid 模组");
        }
        
        MmdSkinClient.logger.info("MMD Skin Fabric 客户端初始化成功");
    }
}
