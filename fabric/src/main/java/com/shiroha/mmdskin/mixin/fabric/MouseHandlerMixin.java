package com.shiroha.mmdskin.mixin.fabric;

import com.shiroha.mmdskin.renderer.camera.MMDCameraController;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MouseHandler Mixin — 舞台播放期间拦截鼠标按钮事件
 * 
 * PLAYING 状态下：
 *   右键(button=1) → 切换鼠标释放/捕获
 *   其他按钮 → 忽略（不触发攻击/使用物品/grabMouse）
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void onStageMousePress(long window, int button, int action, int mods, CallbackInfo ci) {
        MMDCameraController controller = MMDCameraController.getInstance();
        if (!controller.isPlaying()) return;
        
        // action == 1 → GLFW_PRESS
        if (action == 1 && button == 1) {
            controller.toggleMouseGrab();
        }
        // 舞台播放期间拦截所有鼠标按钮事件，防止 grabMouse / startUseItem / startAttack
        ci.cancel();
    }
}
