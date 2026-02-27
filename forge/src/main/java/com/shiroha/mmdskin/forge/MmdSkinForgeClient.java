package com.shiroha.mmdskin.forge;

import com.shiroha.mmdskin.MmdSkin;
import com.shiroha.mmdskin.MmdSkinClient;
import com.shiroha.mmdskin.forge.config.MmdSkinConfig;
import com.shiroha.mmdskin.forge.maid.MaidRenderEventHandler;
import com.shiroha.mmdskin.forge.register.MmdSkinRegisterClient;
import com.shiroha.mmdskin.renderer.model.MMDModelOpenGL;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge 客户端初始化
 * 
 * 重构说明：
 * - 初始化统一配置管理器
 * - 使用 ConfigManager 访问配置
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD, modid = MmdSkin.MOD_ID)
public class MmdSkinForgeClient {
    /**
     * MOD 事件：注册按键映射（在 FMLClientSetupEvent 之前触发）
     */
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        MmdSkinRegisterClient.onRegisterKeyMappings(event);
    }
    
    /**
     * MOD 事件：注册实体渲染器（在 FMLClientSetupEvent 之前触发）
     */
    @SubscribeEvent
    public static void onRegisterEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        MmdSkinRegisterClient.onRegisterEntityRenderers(event);
    }
    
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        MmdSkinConfig.init();
        MmdSkinClient.initClient();
        MmdSkinRegisterClient.Register();
        MMDModelOpenGL.isMMDShaderEnabled = com.shiroha.mmdskin.config.ConfigManager.isMMDShaderEnabled();
        MinecraftForge.EVENT_BUS.register(new MaidRenderEventHandler());
    }
}
