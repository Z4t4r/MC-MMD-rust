package com.shiroha.mmdskin.forge.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.shiroha.mmdskin.forge.register.MmdSkinRegisterCommon;
import com.shiroha.mmdskin.maid.MaidMMDModelManager;
import com.shiroha.mmdskin.renderer.animation.PendingAnimSignalCache;
import com.shiroha.mmdskin.renderer.render.MmdSkinRendererPlayerHelper;
import com.shiroha.mmdskin.renderer.render.MorphSyncHelper;
import com.shiroha.mmdskin.renderer.render.StageAnimSyncHelper;
import com.shiroha.mmdskin.ui.network.NetworkOpCode;
import com.shiroha.mmdskin.ui.network.PlayerModelSyncManager;
import com.shiroha.mmdskin.ui.network.ServerModelRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Forge 网络包序列化与处理
 */
public class MmdSkinNetworkPack {
    private static final Logger logger = LogManager.getLogger();

    public int opCode;
    public UUID playerUUID;
    public String animId;
    public int arg0;

    public MmdSkinNetworkPack(int opCode, UUID playerUUID, String animId) {
        this.opCode = opCode;
        this.playerUUID = playerUUID;
        this.animId = animId;
        this.arg0 = 0;
    }

    public MmdSkinNetworkPack(int opCode, UUID playerUUID, int arg0) {
        this.opCode = opCode;
        this.playerUUID = playerUUID;
        this.animId = "";
        this.arg0 = arg0;
    }

    public MmdSkinNetworkPack(int opCode, UUID playerUUID, int entityId, String modelName) {
        this.opCode = opCode;
        this.playerUUID = playerUUID;
        this.animId = modelName;
        this.arg0 = entityId;
    }

    /** 从缓冲区反序列化 */
    public MmdSkinNetworkPack(FriendlyByteBuf buffer) {
        opCode = buffer.readInt();
        playerUUID = buffer.readUUID();

        if (NetworkOpCode.isStringPayload(opCode)) {
            animId = buffer.readUtf();
            arg0 = 0;
        } else if (NetworkOpCode.isEntityStringPayload(opCode)) {
            arg0 = buffer.readInt();
            animId = buffer.readUtf();
        } else {
            animId = "";
            arg0 = buffer.readInt();
        }
    }

    /** 序列化到缓冲区 */
    public void pack(FriendlyByteBuf buffer) {
        buffer.writeInt(opCode);
        buffer.writeUUID(playerUUID);

        if (NetworkOpCode.isStringPayload(opCode)) {
            buffer.writeUtf(animId);
        } else if (NetworkOpCode.isEntityStringPayload(opCode)) {
            buffer.writeInt(arg0);
            buffer.writeUtf(animId);
        } else {
            buffer.writeInt(arg0);
        }
    }

    /** 服务端/客户端统一入口 */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide() == net.minecraftforge.fml.LogicalSide.CLIENT) {
                doInClient();
            } else {
                handleOnServer(ctx.get());
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /** 服务端处理：鉴权 + opCode 10 回传 + 转发 */
    private void handleOnServer(NetworkEvent.Context ctx) {
        ServerPlayer sender = ctx.getSender();
        if (sender == null) return;

        // 鉴权：声称的 UUID 必须与实际发送者一致
        if (!sender.getUUID().equals(playerUUID)) {
            logger.warn("UUID 不匹配，丢弃数据包: claimed={}, real={}", playerUUID, sender.getUUID());
            return;
        }

        // 模型选择时更新服务端注册表
        if (opCode == NetworkOpCode.MODEL_SELECT) {
            ServerModelRegistry.updateModel(playerUUID, animId);
        }

        // opCode 10：回传所有已注册模型给请求者，不转发
        if (opCode == NetworkOpCode.REQUEST_ALL_MODELS) {
            ServerModelRegistry.sendAllTo((modelOwnerUUID, modelName) ->
                MmdSkinRegisterCommon.channel.send(
                    PacketDistributor.PLAYER.with(() -> sender),
                    new MmdSkinNetworkPack(NetworkOpCode.MODEL_SELECT, modelOwnerUUID, modelName)));
            return;
        }

        // 转发给所有客户端（Forge 的 ALL 包含发送者，客户端 doInClient 会自行过滤）
        MmdSkinRegisterCommon.channel.send(PacketDistributor.ALL.noArg(), this);
    }

    /** 客户端处理 */
    private void doInClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (playerUUID.equals(mc.player.getUUID())) return;
        if (mc.level == null) return;

        Player target = mc.level.getPlayerByUUID(playerUUID);

        switch (opCode) {
            case NetworkOpCode.CUSTOM_ANIM -> {
                if (target != null) MmdSkinRendererPlayerHelper.CustomAnim(target, animId);
                com.shiroha.mmdskin.ui.follow.RemoteAnimCache.put(playerUUID, animId);
            }
            case NetworkOpCode.RESET_PHYSICS -> {
                if (target != null) {
                    MmdSkinRendererPlayerHelper.ResetPhysics(target);
                } else {
                    PendingAnimSignalCache.put(playerUUID, PendingAnimSignalCache.SignalType.RESET);
                }
            }
            case NetworkOpCode.MODEL_SELECT -> {
                PlayerModelSyncManager.onRemotePlayerModelReceived(playerUUID, animId);
            }
            case NetworkOpCode.MAID_MODEL -> {
                Entity maidEntity = mc.level.getEntity(arg0);
                if (maidEntity != null) MaidMMDModelManager.bindModel(maidEntity.getUUID(), animId);
            }
            case NetworkOpCode.MAID_ACTION -> {
                Entity maidEntity = mc.level.getEntity(arg0);
                if (maidEntity != null) MaidMMDModelManager.playAnimation(maidEntity.getUUID(), animId);
            }
            case NetworkOpCode.MORPH_SYNC -> {
                if (target != null) MorphSyncHelper.applyRemoteMorph(target, animId);
            }
            case NetworkOpCode.STAGE_START -> {
                if (target != null) StageAnimSyncHelper.startStageAnim(target, animId);
            }
            case NetworkOpCode.STAGE_END -> {
                if (target != null) {
                    StageAnimSyncHelper.endStageAnim(target);
                } else {
                    PendingAnimSignalCache.put(playerUUID, PendingAnimSignalCache.SignalType.STAGE_END);
                }
            }
            case NetworkOpCode.STAGE_AUDIO -> {
                if (target != null) MmdSkinRendererPlayerHelper.StageAudioPlay(target, animId);
            }
            case NetworkOpCode.STAGE_MULTI -> {
                com.shiroha.mmdskin.ui.network.StageMultiHandler.handle(playerUUID, animId);
            }
            default -> {}
        }
    }
}
