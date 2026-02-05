package com.shiroha.mmdskin.fabric.register;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class MmdSkinRegisterCommon {
    public static ResourceLocation SKIN_C2S = new ResourceLocation("3d-skin", "network_c2s");
    public static ResourceLocation SKIN_S2C = new ResourceLocation("3d-skin", "network_s2c");

    public static void Register() {
        ServerPlayNetworking.registerGlobalReceiver(SKIN_C2S, (server, player, handler, buf, responseSender) -> {
            // 读取 opCode 以决定如何转发
            int opCode = buf.readInt();
            java.util.UUID playerUUID = buf.readUUID();
            
            // 根据 opCode 读取不同格式的数据
            FriendlyByteBuf packetbuf = PacketByteBufs.create();
            packetbuf.writeInt(opCode);
            packetbuf.writeUUID(playerUUID);
            
            if (opCode == 1 || opCode == 3) {
                // opCode 1: 动作执行, opCode 3: 模型选择同步 - 字符串参数
                String data = buf.readUtf();
                packetbuf.writeUtf(data);
            } else if (opCode == 4 || opCode == 5) {
                // opCode 4/5: 女仆模型/动作 - entityId + 字符串
                int entityId = buf.readInt();
                String data = buf.readUtf();
                packetbuf.writeInt(entityId);
                packetbuf.writeUtf(data);
            } else {
                // 其他: 整数参数
                int arg0 = buf.readInt();
                packetbuf.writeInt(arg0);
            }
            
            server.execute(() -> {
                for(ServerPlayer serverPlayer : PlayerLookup.all(server)){
                    if(!serverPlayer.equals(player)){
                        // 为每个玩家创建新的缓冲区（避免重复发送问题）
                        FriendlyByteBuf copyBuf = PacketByteBufs.copy(packetbuf);
                        ServerPlayNetworking.send(serverPlayer, MmdSkinRegisterCommon.SKIN_S2C, copyBuf);
                    }
                }
            });
        });
    }
}
