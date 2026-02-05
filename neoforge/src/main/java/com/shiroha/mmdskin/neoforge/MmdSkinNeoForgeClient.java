package com.shiroha.mmdskin.neoforge;

import com.shiroha.mmdskin.neoforge.register.MmdSkinRegisterClient;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MmdSkinNeoForgeClient.MOD_ID)
public class MmdSkinNeoForgeClient {
    public static final String MOD_ID = "mmdskin";

    public MmdSkinNeoForgeClient(IEventBus modEventBus) {
        // 注册客户端设置事件
        modEventBus.addListener(this::onClientSetup);
    }

    @net.neoforged.api.distmarker.OnlyIn(value = Dist.CLIENT, _side = net.neoforged.api.distmarker.Dist.CLIENT)
    private void onClientSetup(final FMLClientSetupEvent event) {
        // 初始化客户端注册
        MmdSkinRegisterClient.Register();
    }
}
