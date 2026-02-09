package com.shiroha.mmdskin.mixin.fabric;

import com.shiroha.mmdskin.renderer.camera.MMDCameraController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 相机 Mixin — 舞台模式相机接管
 * 在 Camera.setup() 尾部覆盖位置和旋转
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    
    @Shadow
    protected abstract void setPosition(double x, double y, double z);
    
    @Shadow
    protected abstract void setRotation(float yaw, float pitch);
    
    @Inject(method = "setup", at = @At("TAIL"))
    private void onSetup(BlockGetter level, Entity entity, boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        MMDCameraController controller = MMDCameraController.getInstance();
        if (controller.isActive()) {
            controller.checkEscapeKey();
            if (!controller.isActive()) return;
            controller.updateCamera();
            if (!controller.isActive()) return;
            setPosition(controller.getCameraX(), controller.getCameraY(), controller.getCameraZ());
            setRotation(controller.getCameraYaw(), controller.getCameraPitch());
        }
    }
}
