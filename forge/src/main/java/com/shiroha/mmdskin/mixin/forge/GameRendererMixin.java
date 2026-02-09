package com.shiroha.mmdskin.mixin.forge;

import com.shiroha.mmdskin.renderer.camera.MMDCameraController;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GameRenderer Mixin — 舞台模式 FOV 覆盖
 * 当舞台模式激活时，用 VMD 相机的 FOV 覆盖游戏 FOV
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        MMDCameraController controller = MMDCameraController.getInstance();
        if (controller.isActive()) {
            cir.setReturnValue((double) controller.getCameraFov());
        }
    }
}
