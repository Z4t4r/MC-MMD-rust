package com.shiroha.mmdskin.forge.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.shiroha.mmdskin.forge.register.MmdSkinRegisterCommon;
import com.shiroha.mmdskin.maid.MaidMMDModelManager;
import com.shiroha.mmdskin.renderer.render.MmdSkinRendererPlayerHelper;
import com.shiroha.mmdskin.ui.network.PlayerModelSyncManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Forge 网络包处理
 * 支持动作同步和物理重置
 * 
 * 修改说明：
 * - 添加字符串参数支持，用于传输动画ID（支持中文文件名）
 * - 移除旧的 EntityPlayer_ 前缀查找逻辑
 * - 使用配置系统获取玩家模型
 */
public class MmdSkinNetworkPack {
    public int opCode;
    public UUID playerUUID;
    public String animId; // 动画ID（字符串）
    public int arg0;      // 整数参数（向后兼容）

    /**
     * 构造函数（字符串参数）
     */
    public MmdSkinNetworkPack(int opCode, UUID playerUUID, String animId) {
        this.opCode = opCode;
        this.playerUUID = playerUUID;
        this.animId = animId;
        this.arg0 = 0;
    }

    /**
     * 构造函数（整数参数）
     */
    public MmdSkinNetworkPack(int opCode, UUID playerUUID, int arg0) {
        this.opCode = opCode;
        this.playerUUID = playerUUID;
        this.animId = "";
        this.arg0 = arg0;
    }
    
    /**
     * 构造函数（女仆模型变更：entityId + modelName）
     */
    public MmdSkinNetworkPack(int opCode, UUID playerUUID, int entityId, String modelName) {
        this.opCode = opCode;
        this.playerUUID = playerUUID;
        this.animId = modelName;
        this.arg0 = entityId;
    }

    /**
     * 从缓冲区读取
     */
    public MmdSkinNetworkPack(FriendlyByteBuf buffer) {
        opCode = buffer.readInt();
        playerUUID = new UUID(buffer.readLong(), buffer.readLong());
        
        // 根据 opCode 决定读取字符串还是整数
        if (opCode == 1 || opCode == 3) {
            // opCode 1: 动作执行, opCode 3: 模型选择同步
            animId = buffer.readUtf();
            arg0 = 0;
        } else if (opCode == 4 || opCode == 5) {
            arg0 = buffer.readInt();   // 女仆模型/动作变更，读取 entityId
            animId = buffer.readUtf(); // 读取 modelName/animId
        } else {
            animId = "";
            arg0 = buffer.readInt(); // 其他操作，读取整数
        }
    }

    /**
     * 写入缓冲区
     */
    public void Pack(FriendlyByteBuf buffer) {
        buffer.writeInt(opCode);
        buffer.writeLong(playerUUID.getMostSignificantBits());
        buffer.writeLong(playerUUID.getLeastSignificantBits());
        
        // 根据 opCode 决定写入字符串还是整数
        if (opCode == 1 || opCode == 3) {
            // opCode 1: 动作执行, opCode 3: 模型选择同步
            buffer.writeUtf(animId);
        } else if (opCode == 4 || opCode == 5) {
            buffer.writeInt(arg0);   // 女仆模型/动作变更，写入 entityId
            buffer.writeUtf(animId); // 写入 modelName/animId
        } else {
            buffer.writeInt(arg0); // 其他操作，写入整数
        }
    }

    /**
     * 处理网络包
     */
    public void Do(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                DoInClient();
            } else {
                // 服务器端：转发给所有客户端
                MmdSkinRegisterCommon.channel.send(PacketDistributor.ALL.noArg(), this);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 客户端处理
     */
    public void DoInClient() {
        Minecraft MCinstance = Minecraft.getInstance();
        if (MCinstance.player == null) return;
        // 忽略自己发送的消息
        if (playerUUID.equals(MCinstance.player.getUUID()))
            return;
            
        if (MCinstance.level == null) return;
        Player target = MCinstance.level.getPlayerByUUID(playerUUID);
            
        switch (opCode) {
            case 1: {
                // 执行动画（使用字符串ID）
                MmdSkinRendererPlayerHelper.CustomAnim(target, animId);
                break;
            }
            case 3: {
                // 模型选择同步
                PlayerModelSyncManager.onRemotePlayerModelReceived(playerUUID, animId);
                break;
            }
            case 2: {
                // 重置物理
                MmdSkinRendererPlayerHelper.ResetPhysics(target);
                break;
            }
            case 4: {
                // 女仆模型变更
                Entity maidEntity = MCinstance.level.getEntity(arg0);
                if (maidEntity != null) {
                    MaidMMDModelManager.bindModel(maidEntity.getUUID(), animId);
                }
                break;
            }
            case 5: {
                // 女仆动作变更
                Entity maidEntity = MCinstance.level.getEntity(arg0);
                if (maidEntity != null) {
                    MaidMMDModelManager.playAnimation(maidEntity.getUUID(), animId);
                }
                break;
            }
        }
    }
}
