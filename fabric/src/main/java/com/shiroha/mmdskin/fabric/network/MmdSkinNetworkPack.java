package com.shiroha.mmdskin.fabric.network;

import java.util.UUID;

import com.shiroha.mmdskin.fabric.register.MmdSkinRegisterCommon;
import com.shiroha.mmdskin.maid.MaidMMDModelManager;
import com.shiroha.mmdskin.renderer.render.MmdSkinRendererPlayerHelper;
import com.shiroha.mmdskin.ui.network.PlayerModelSyncManager;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric 网络包处理
 * 支持动作同步和物理重置
 */
public class MmdSkinNetworkPack {
    /**
     * 发送到服务器（整数参数版本，用于向后兼容）
     */
    public static void sendToServer(int opCode, UUID playerUUID, int arg0){
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeInt(opCode);
        buffer.writeUUID(playerUUID);
        buffer.writeInt(arg0);
        ClientPlayNetworking.send(MmdSkinRegisterCommon.SKIN_C2S, buffer);
    }
    
    /**
     * 发送到服务器（字符串参数版本，用于动画ID）
     */
    public static void sendToServer(int opCode, UUID playerUUID, String animId){
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeInt(opCode);
        buffer.writeUUID(playerUUID);
        buffer.writeUtf(animId); // 使用 UTF-8 编码支持中文
        ClientPlayNetworking.send(MmdSkinRegisterCommon.SKIN_C2S, buffer);
    }
    
    /**
     * 发送到服务器（entityId + 字符串参数版本，用于女仆模型/动作）
     */
    public static void sendToServer(int opCode, UUID playerUUID, int entityId, String data){
        FriendlyByteBuf buffer = PacketByteBufs.create();
        buffer.writeInt(opCode);
        buffer.writeUUID(playerUUID);
        buffer.writeInt(entityId);
        buffer.writeUtf(data);
        ClientPlayNetworking.send(MmdSkinRegisterCommon.SKIN_C2S, buffer);
    }
    
    public static void DoInClient(FriendlyByteBuf buffer){
        int opCode = buffer.readInt();
        UUID playerUUID = buffer.readUUID();
        
        // 根据 opCode 决定读取格式
        if (opCode == 1 || opCode == 3) {
            // opCode 1: 动作执行，opCode 3: 模型选择同步
            String data = buffer.readUtf();
            DoInClientString(opCode, playerUUID, data);
        } else if (opCode == 4 || opCode == 5) {
            // 女仆模型/动作变更，读取 entityId + string
            int entityId = buffer.readInt();
            String data = buffer.readUtf();
            DoInClientMaid(opCode, playerUUID, entityId, data);
        } else {
            // 其他操作，读取整数
            int arg0 = buffer.readInt();
            DoInClientInt(opCode, playerUUID, arg0);
        }
    }

    /**
     * 客户端处理（整数参数）
     */
    public static void DoInClientInt(int opCode, UUID playerUUID, int arg0) {
        Minecraft MCinstance = Minecraft.getInstance();
        if (MCinstance.player == null) return;
        if (playerUUID.equals(MCinstance.player.getUUID())) return;
            
        switch (opCode) {
            case 2: {
                // 重置物理
                if (MCinstance.level == null) return;
                Player target = MCinstance.level.getPlayerByUUID(playerUUID);
                if (target != null)
                    MmdSkinRendererPlayerHelper.ResetPhysics(target);
                break;
            }
        }
    }
    
    /**
     * 客户端处理（字符串参数）
     */
    public static void DoInClientString(int opCode, UUID playerUUID, String data) {
        Minecraft MCinstance = Minecraft.getInstance();
        if (MCinstance.player == null) return;
        if (playerUUID.equals(MCinstance.player.getUUID())) return;
            
        switch (opCode) {
            case 1: {
                // 执行动画
                if (MCinstance.level == null) return;
                Player target = MCinstance.level.getPlayerByUUID(playerUUID);
                if (target != null)
                    MmdSkinRendererPlayerHelper.CustomAnim(target, data);
                break;
            }
            case 3: {
                // 模型选择同步
                PlayerModelSyncManager.onRemotePlayerModelReceived(playerUUID, data);
                break;
            }
        }
    }
    
    /**
     * 客户端处理（女仆相关：entityId + 字符串参数）
     */
    public static void DoInClientMaid(int opCode, UUID playerUUID, int entityId, String data) {
        Minecraft MCinstance = Minecraft.getInstance();
        if (MCinstance.player == null) return;
        if (playerUUID.equals(MCinstance.player.getUUID())) return;
        if (MCinstance.level == null) return;
        
        Entity maidEntity = MCinstance.level.getEntity(entityId);
        if (maidEntity == null) return;
        
        switch (opCode) {
            case 4: {
                // 女仆模型变更
                MaidMMDModelManager.bindModel(maidEntity.getUUID(), data);
                break;
            }
            case 5: {
                // 女仆动作变更
                MaidMMDModelManager.playAnimation(maidEntity.getUUID(), data);
                break;
            }
        }
    }
}
