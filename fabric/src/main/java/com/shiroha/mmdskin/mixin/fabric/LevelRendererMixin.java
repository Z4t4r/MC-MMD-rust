package com.shiroha.mmdskin.mixin.fabric;

import com.shiroha.mmdskin.fabric.YsmCompat;
import com.shiroha.mmdskin.renderer.core.FirstPersonManager;
import com.shiroha.mmdskin.renderer.core.IrisCompat;
import com.shiroha.mmdskin.ui.network.PlayerModelSyncManager;
import net.minecraft.client.Camera;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * LevelRenderer Mixin — 第一人称 MMD 模型渲染
 *
 * 在第一人称模式下，Minecraft 默认跳过渲染本地玩家实体。
 * 此 Mixin 通过在 renderLevel 方法内将 Camera.isDetached() 重定向为 true，
 * 使实体渲染循环不再跳过本地玩家，从而触发 PlayerRendererMixin 的 MMD 模型渲染。
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Redirect(
        method = "renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z", ordinal = 0)
    )
    private boolean onCameraIsDetached(Camera camera) {
        if (FirstPersonManager.shouldRenderFirstPerson() && !IrisCompat.isRenderingShadows()) {
            // 获取当前选中的模型
            Entity entity = camera.getEntity();
            if (entity instanceof AbstractClientPlayer player) {
                String playerName = player.getName().getString();
                String selectedModel = PlayerModelSyncManager.getPlayerModel(player.getUUID(), playerName, true);

                // 检查是否为 MMD 默认/原版模型状态
                boolean isMmdDefault = selectedModel == null || selectedModel.isEmpty() || selectedModel.equals("默认 (原版渲染)");
                boolean isMmdActive = !isMmdDefault;
                boolean isVanilaMmdModel = isMmdActive && (selectedModel.equals("VanilaModel") || selectedModel.equalsIgnoreCase("vanila"));

                // 核心逻辑调整：
                // 1. 如果 YSM 插件处于激活状态
                if (YsmCompat.isYsmModelActive(player)) {
                    // 如果 YSM 开启了“阻止自身渲染”，此时用户希望：
                    // - 渲染 MMD 模型（如果是非原版 MMD 模型）
                    // - 不产生影子
                    if (YsmCompat.isDisableSelfModel()) {
                        if (isMmdActive && !isVanilaMmdModel) {
                            // 俯角检查：向上看时隐藏模型（防止看到后脑勺）
                            if (camera.getXRot() < 0) {
                                return false;
                            }
                            // 此时我们需要返回 true 来让渲染流跑进 PlayerRenderer，
                            // 但在 PlayerRendererMixin 里我们会拦截掉原版，只画 MMD。
                            return true;
                        }
                        // 如果是原版 MMD 模型且开启了阻止渲染，则彻底隐藏
                        return false;
                    } else {
                        // 如果 YSM 关闭了“阻止自身渲染”，用户希望：
                        // - 渲染 YSM 模型（不渲染 MMD）
                        // - 不在第一人称渲染（YSM 自己的逻辑会处理），且不要产生影子
                        // 为了不要产生影子，在第一人称下我们应该返回 false 让原版剔除实体
                        return false;
                    }
                }

                // 2. 如果没有 YSM，再由 MMD 决定：
                if (isMmdActive && !isVanilaMmdModel) {
                    if (camera.getXRot() < 0) {
                        return false;
                    }
                    return true;
                }
            }
        }
        return camera.isDetached();
    }
}
