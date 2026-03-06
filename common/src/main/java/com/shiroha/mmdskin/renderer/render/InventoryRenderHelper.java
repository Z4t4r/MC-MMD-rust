package com.shiroha.mmdskin.renderer.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shiroha.mmdskin.renderer.core.IMMDModel;
import com.shiroha.mmdskin.renderer.core.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 库存屏幕渲染辅助类
 * 负责在库存界面中渲染 3D 模型
 */
public class InventoryRenderHelper {
    
    /**
     * 检查当前是否在库存屏幕
     * 直接检测 Minecraft.screen 类型，无需栈帧分析
     */
    public static boolean isInventoryScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null) return false;
        String className = mc.screen.getClass().getName();
        return className.contains("InventoryScreen") || className.contains("class_490");
    }
    
    /**
     * 在库存屏幕中渲染模型
     */
    public static void renderInInventory(AbstractClientPlayer player, IMMDModel model, float entityYaw, 
                                        float tickDelta, PoseStack matrixStack, int packedLight, float[] size) {
        // MC 1.21.1: 使用传入的 PoseStack 而非 RenderSystem.getModelViewStack()
        matrixStack.pushPose();
        
        float inventorySize = size[1];
        matrixStack.scale(inventorySize, inventorySize, inventorySize);
        matrixStack.scale(20.0f, 20.0f, -20.0f);
        
        Quaternionf rotation = calculateRotation(player);
        matrixStack.mulPose(rotation);
        
        RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
        model.render(player, entityYaw, 0.0f, new Vector3f(0.0f), tickDelta, matrixStack, packedLight, RenderContext.INVENTORY);
        
        matrixStack.popPose();
        
        Quaternionf bodyRotation = new Quaternionf().rotateY(-player.yBodyRot * ((float)Math.PI / 180F));
        matrixStack.mulPose(bodyRotation);
        matrixStack.scale(inventorySize, inventorySize, inventorySize);
        matrixStack.scale(0.09f, 0.09f, 0.09f);
    }
    
    private static Quaternionf calculateRotation(AbstractClientPlayer player) {
        Quaternionf quaternion = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf pitch = new Quaternionf().rotateX(-player.getXRot() * ((float)Math.PI / 180F));
        Quaternionf yaw = new Quaternionf().rotateY(-player.yBodyRot * ((float)Math.PI / 180F));
        
        quaternion.mul(pitch);
        quaternion.mul(yaw);
        
        return quaternion;
    }
    
}
