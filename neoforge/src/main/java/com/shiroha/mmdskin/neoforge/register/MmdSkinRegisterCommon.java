package com.shiroha.mmdskin.neoforge.register;

import com.shiroha.mmdskin.neoforge.network.MmdSkinNetworkPack;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import com.shiroha.mmdskin.MmdSkin;

/**
 * NeoForge 通用注册
 * 负责网络通信注册
 */
@Mod.EventBusSubscriber(modid = MmdSkin.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MmdSkinRegisterCommon {

    /**
     * 注册网络处理器
     */
    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlerEvent event) {
        final PayloadRegistrar registrar = event.registrar(MmdSkin.MOD_ID)
            .versioned("1")
            .optionalNetwork();

        // 注册网络包处理器
        registrar.play(MmdSkinNetworkPack.EXAMPLE_PACKET_ID,
            MmdSkinNetworkPack.ExamplePayload::new,
            MmdSkinNetworkPack.ClientPayloadHandler::new,
            MmdSkinNetworkPack.ServerPayloadHandler::new);
    }

    public static void Register() {
        // NeoForge 使用新式网络系统，通过事件总线注册
        // 具体实现见 MmdSkinNetworkPack
    }
}
