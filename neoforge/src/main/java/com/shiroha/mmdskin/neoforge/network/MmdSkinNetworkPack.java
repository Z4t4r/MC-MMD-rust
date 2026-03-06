package com.shiroha.mmdskin.neoforge.network;

import java.util.UUID;

import com.shiroha.mmdskin.MmdSkin;
import com.shiroha.mmdskin.maid.MaidMMDModelManager;
import com.shiroha.mmdskin.neoforge.register.MmdSkinAttachments;
import com.shiroha.mmdskin.renderer.render.MmdSkinRendererPlayerHelper;
import com.shiroha.mmdskin.renderer.render.MorphSyncHelper;
import com.shiroha.mmdskin.renderer.render.StageAnimSyncHelper;
import com.shiroha.mmdskin.ui.network.PlayerModelSyncManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * NeoForge 网络包处理
 * 使用 NeoForge 1.21.1 的 Payload 系统
 * 支持动作同步和物理重置
 */
public record MmdSkinNetworkPack(int opCode, UUID playerUUID, String animId, int arg0) implements CustomPacketPayload {
    
    public static final Type<MmdSkinNetworkPack> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MmdSkin.MOD_ID, "network_pack"));
    
    public static final StreamCodec<FriendlyByteBuf, MmdSkinNetworkPack> STREAM_CODEC = StreamCodec.of(
        MmdSkinNetworkPack::encode,
        MmdSkinNetworkPack::decode
    );
    
    // 工厂方法（字符串参数）
    public static MmdSkinNetworkPack withAnimId(int opCode, UUID playerUUID, String animId) {
        return new MmdSkinNetworkPack(opCode, playerUUID, animId, 0);
    }
    
    // 工厂方法（整数参数）
    public static MmdSkinNetworkPack withArg(int opCode, UUID playerUUID, int arg0) {
        return new MmdSkinNetworkPack(opCode, playerUUID, "", arg0);
    }
    
    // 工厂方法（女仆模型变更）
    public static MmdSkinNetworkPack forMaid(int opCode, UUID playerUUID, int entityId, String modelName) {
        return new MmdSkinNetworkPack(opCode, playerUUID, modelName, entityId);
    }
    
    private static void encode(FriendlyByteBuf buffer, MmdSkinNetworkPack pack) {
        buffer.writeInt(pack.opCode);
        buffer.writeUUID(pack.playerUUID);
        if (pack.opCode == 1 || pack.opCode == 3 || pack.opCode == 6 || pack.opCode == 7 || pack.opCode == 8 || pack.opCode == 9) {
            buffer.writeUtf(pack.animId);
        } else if (pack.opCode == 4 || pack.opCode == 5) {
            buffer.writeInt(pack.arg0);
            buffer.writeUtf(pack.animId);
        } else {
            buffer.writeInt(pack.arg0);
        }
    }

    private static MmdSkinNetworkPack decode(FriendlyByteBuf buffer) {
        int opCode = buffer.readInt();
        UUID playerUUID = buffer.readUUID();
        String animId = "";
        int arg0 = 0;

        if (opCode == 1 || opCode == 3 || opCode == 6 || opCode == 7 || opCode == 8 || opCode == 9) {
            animId = buffer.readUtf();
        } else if (opCode == 4 || opCode == 5) {
            arg0 = buffer.readInt();
            animId = buffer.readUtf();
        } else {
            arg0 = buffer.readInt();
        }
        return new MmdSkinNetworkPack(opCode, playerUUID, animId, arg0);
    }
    
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    
    public static void handle(MmdSkinNetworkPack pack, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sender) {
                // opCode 10: 客户端请求所有玩家的模型同步
                if (pack.opCode == 10) {
                    for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
                        String modelName = player.getData(MmdSkinAttachments.PLAYER_MMD_MODEL.get());
                        if (modelName != null && !modelName.isEmpty()) {
                            PacketDistributor.sendToPlayer(sender, MmdSkinNetworkPack.withAnimId(3, player.getUUID(), modelName));
                        }
                    }
                    return;
                }

                if (pack.opCode == 4) {
                    Entity entity = sender.level().getEntity(pack.arg0);
                    if (entity != null) {
                        entity.setData(MmdSkinAttachments.MAID_MMD_MODEL.get(), pack.animId);
                    }
                }

                // opCode 3: 模型变更，存入附件系统
                if (pack.opCode == 3) {
                    sender.setData(MmdSkinAttachments.PLAYER_MMD_MODEL.get(), pack.animId);
                }

                // 服务器端（含局域网）：转发给所有客户端（包括发送者，以便同步状态）
                for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
                    PacketDistributor.sendToPlayer(player, pack);
                }
            } else {
                // 客户端：处理收到的包
                pack.handleClient();
            }
        });
    }
    
    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (this.opCode == 4 || this.opCode == 5 || !this.playerUUID.equals(mc.player.getUUID())) {
                if (mc.level != null) {
                    Player target = mc.level.getPlayerByUUID(this.playerUUID);
                    switch (this.opCode) {
                        case 1:
                            if (target != null) {
                                MmdSkinRendererPlayerHelper.CustomAnim(target, this.animId);
                            }
                            break;
                        case 2:
                            if (target != null) {
                                MmdSkinRendererPlayerHelper.ResetPhysics(target);
                            }
                            break;
                        case 3:
                            PlayerModelSyncManager.onRemotePlayerModelReceived(this.playerUUID, this.animId);
                            break;
                        case 4:
                            Entity maidEntityx = mc.level.getEntity(this.arg0);
                            if (maidEntityx != null) {
                                MaidMMDModelManager.bindModel(maidEntityx.getUUID(), this.animId);
                            }
                            break;
                        case 5:
                            Entity maidEntity = mc.level.getEntity(this.arg0);
                            if (maidEntity != null) {
                                MaidMMDModelManager.playAnimation(maidEntity.getUUID(), this.animId);
                            }
                            break;
                        case 6:
                            if (target != null) {
                                MorphSyncHelper.applyRemoteMorph(target, this.animId);
                            }
                            break;
                        case 7:
                            if (target != null) {
                                StageAnimSyncHelper.startStageAnim(target, this.animId);
                            }
                            break;
                        case 8:
                            if (target != null) {
                                StageAnimSyncHelper.endStageAnim(target);
                            }
                            break;
                        case 9:
                            if (target != null) {
                                MmdSkinRendererPlayerHelper.StageAudioPlay(target, this.animId);
                            }
                            break;
                    }
                }
            }
        }
    }
}
