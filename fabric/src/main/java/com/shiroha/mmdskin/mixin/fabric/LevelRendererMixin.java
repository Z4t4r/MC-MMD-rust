package com.shiroha.mmdskin.mixin.fabric;

import com.shiroha.mmdskin.compat.vr.VRArmHider;
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
 * LevelRenderer Mixin — 强制渲染本地玩家实体
 * <p>
 * Minecraft 默认在第一人称下跳过本地玩家渲染（camera.isDetached() == false）。
 * 此 Mixin 在以下场景强制返回 true：
 * 1. 第一人称 MMD 模型模式（非 VR）
 * 2. VR 模式下 MMD 模型激活（确保身体可见）
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    
    @Redirect(
        method = "renderLevel",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z", ordinal = 0)
    )
    private boolean onCameraIsDetached(Camera camera) {
        if (IrisCompat.isRenderingShadows()) {
            return camera.isDetached();
        }

        Entity entity = camera.getEntity();
        if (!(entity instanceof AbstractClientPlayer player)) {
            return camera.isDetached();
        }

        String playerName = player.getName().getString();
        String selectedModel = PlayerModelSyncManager.getPlayerModel(player.getUUID(), playerName, true);

        boolean isMmdDefault = selectedModel == null || selectedModel.isEmpty()
                || selectedModel.equals("默认 (原版渲染)");
        boolean isMmdActive = !isMmdDefault;
        boolean isVanilaMmdModel = isMmdActive && (selectedModel.equals("VanillaModel")
                || selectedModel.equalsIgnoreCase("vanilla")
                || selectedModel.equals("VanilaModel")
                || selectedModel.equalsIgnoreCase("vanila"));

        // VR 模式：MMD 模型激活时强制渲染身体
        if (isMmdActive && !isVanilaMmdModel && VRArmHider.isLocalPlayerInVR()) {
            return true;
        }

        // 非 VR：第一人称模型逻辑
        if (FirstPersonManager.shouldRenderFirstPerson() && isMmdActive && !isVanilaMmdModel) {
            // YSM 兼容
            if (YsmCompat.isYsmModelActive(player)) {
                if (YsmCompat.isDisableSelfModel()) {
                    return camera.getXRot() >= 0;
                }
                return false;
            }
            // 俯角检查：向上看时隐藏（防止看到后脑勺）
            return camera.getXRot() >= 0;
        }

        return camera.isDetached();
    }
}
