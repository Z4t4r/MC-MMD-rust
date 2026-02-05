package com.shiroha.mmdskin.neoforge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.shiroha.mmdskin.MmdSkin;
import com.shiroha.mmdskin.neoforge.register.MmdSkinRegisterCommon;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MmdSkin.MOD_ID)
public class MmdSkinNeoForge {
    public static final Logger logger = LogManager.getLogger();

    public MmdSkinNeoForge() {
        // 获取模组事件总线
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册通用设置事件
        modEventBus.addListener(this::preInit);

        // 注册到 NeoForge 游戏事件总线（用于游戏内事件）
        NeoForge.EVENT_BUS.register(this);
    }

    public void preInit(FMLCommonSetupEvent event) {
        logger.info("MMD Skin (NeoForge) 预初始化开始...");
        MmdSkinRegisterCommon.Register();
        logger.info("MMD Skin (NeoForge) 预初始化成功");
    }
}
