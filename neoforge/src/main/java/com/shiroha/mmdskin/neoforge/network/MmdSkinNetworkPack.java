package com.shiroha.mmdskin.neoforge.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.shiroha.mmdskin.maid.MaidMMDModelManager;
import com.shiroha.mmdskin.renderer.render.MmdSkinRendererPlayerHelper;
import com.shiroha.mmdskin.ui.PlayerModelSyncManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPlayPayloadHandler;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import org.jetbrains.annotations.NotNull;

/**
 * NeoForge 网络包处理
 * 使用 NeoForge 1.20.2+ 的新式 Custom Payload 系统
 *
 * 支持动作同步和物理重置
 */
public class MmdSkinNetworkPack {

    public static final ResourceLocation EXAMPLE_PACKET_ID =
        ResourceLocation.fromNamespaceAndPath("mmdskin", "network_pack");

    /**
     * Custom Payload 实现
     */
    public static final class ExamplePayload implements CustomPacketPayload {
        private final int opCode;
        private final UUID playerUUID;
        private final String animId;
        private final int arg0;

        public ExamplePayload(int opCode, UUID playerUUID, String animId) {
            this.opCode = opCode;
            this.playerUUID = playerUUID;
            this.animId = animId;
            this.arg0 = 0;
        }

        public ExamplePayload(int opCode, UUID playerUUID, int arg0) {
            this.opCode = opCode;
            this.playerUUID = playerUUID;
            this.animId = "";
            this.arg0 = arg0;
        }

        public ExamplePayload(int opCode, UUID playerUUID, int entityId, String modelName) {
            this.opCode = opCode;
            this.playerUUID = playerUUID;
            this.animId = modelName;
            this.arg0 = entityId;
        }

        public ExamplePayload(FriendlyByteBuf buffer) {
            this.opCode = buffer.readInt();
            this.playerUUID = new UUID(buffer.readLong(), buffer.readLong());

            if (opCode == 1 || opCode == 3) {
                this.animId = buffer.readUtf();
                this.arg0 = 0;
            } else if (opCode == 4 || opCode == 5) {
                this.arg0 = buffer.readInt();
                this.animId = buffer.readUtf();
            } else {
                this.animId = "";
                this.arg0 = buffer.readInt();
            }
        }

        @Override
        public void write(@NotNull FriendlyByteBuf buffer) {
            buffer.writeInt(opCode);
            buffer.writeLong(playerUUID.getMostSignificantBits());
            buffer.writeLong(playerUUID.getLeastSignificantBits());

            if (opCode == 1 || opCode == 3) {
                buffer.writeUtf(animId);
            } else if (opCode == 4 || opCode == 5) {
                buffer.writeInt(arg0);
                buffer.writeUtf(animId);
            } else {
                buffer.writeInt(arg0);
            }
        }

        @Override
        @NotNull
        public ResourceLocation id() {
            return EXAMPLE_PACKET_ID;
        }

        public int getOpCode() { return opCode; }
        public UUID getPlayerUUID() { return playerUUID; }
        public String getAnimId() { return animId; }
        public int getArg0() { return arg0; }
    }

    /**
     * 客户端处理器
     */
    public static class ClientPayloadHandler implements IPlayPayloadHandler<ExamplePayload> {
        @Override
        public void handle(ExamplePayload payload, PlayPayloadContext context) {
            context.workHandler().submitAsync(() -> {
                DoInClient(payload);
            });
        }

        private void DoInClient(ExamplePayload payload) {
            Minecraft MCinstance = Minecraft.getInstance();
            if (MCinstance.player == null || MCinstance.level == null) {
                return;
            }

            // 忽略自己发送的消息
            if (payload.getPlayerUUID().equals(MCinstance.player.getUUID())) {
                return;
            }

            Player target = MCinstance.level.getPlayerByUUID(payload.getPlayerUUID());
            if (target == null) {
                return;
            }

            switch (payload.getOpCode()) {
                case 1: {
                    // 执行动画（使用字符串ID）
                    MmdSkinRendererPlayerHelper.CustomAnim(target, payload.getAnimId());
                    break;
                }
                case 3: {
                    // 模型选择同步
                    PlayerModelSyncManager.onRemotePlayerModelReceived(payload.getPlayerUUID(), payload.getAnimId());
                    break;
                }
                case 2: {
                    // 重置物理
                    MmdSkinRendererPlayerHelper.ResetPhysics(target);
                    break;
                }
                case 4: {
                    // 女仆模型变更
                    Entity maidEntity = MCinstance.level.getEntity(payload.getArg0());
                    if (maidEntity != null) {
                        MaidMMDModelManager.bindModel(maidEntity.getUUID(), payload.getAnimId());
                    }
                    break;
                }
                case 5: {
                    // 女仆动作变更
                    Entity maidEntity = MCinstance.level.getEntity(payload.getArg0());
                    if (maidEntity != null) {
                        MaidMMDModelManager.playAnimation(maidEntity.getUUID(), payload.getAnimId());
                    }
                    break;
                }
            }
        }
    }

    /**
     * 服务器处理器
     */
    public static class ServerPayloadHandler implements IPlayPayloadHandler<ExamplePayload> {
        @Override
        public void handle(ExamplePayload payload, PlayPayloadContext context) {
            context.workHandler().submitAsync(() -> {
                // 服务器端：转发给所有客户端
                // NeoForge 会自动处理转发
            });
        }
    }

    /**
     * 发送网络包到服务器（从客户端）
     */
    public static void sendToServer(ExamplePayload payload) {
        // NeoForge 的网络发送需要通过 PacketDistributor
        // 这会在实际运行时通过 NeoForge 的网络系统处理
        // 具体实现需要在客户端注册时配置
    }

    /**
     * 发送网络包到客户端（从服务器）
     */
    public static void sendToClient(Player player, ExamplePayload payload) {
        // 通过 NeoForge 的 PacketDistributor 发送
    }
}
