package com.shiroha.mmdskin.renderer.render;

import com.shiroha.mmdskin.NativeFunc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.shiroha.mmdskin.renderer.model.MMDModelManager.ModelWithEntityData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 物品渲染辅助类
 * 负责渲染玩家手持物品
 */
public class ItemRenderHelper {
    
    /**
     * 渲染玩家手持物品
     */
    public static void renderItems(AbstractClientPlayer player, ModelWithEntityData model, 
                                   PoseStack matrixStack, MultiBufferSource vertexConsumers, int packedLight) {
        renderMainHandItem(player, model, matrixStack, vertexConsumers, packedLight);
        renderOffHandItem(player, model, matrixStack, vertexConsumers, packedLight);
    }
    
    private static void renderMainHandItem(AbstractClientPlayer player, ModelWithEntityData model,
                                           PoseStack matrixStack, MultiBufferSource vertexConsumers, int packedLight) {
        NativeFunc nf = NativeFunc.GetInst();
        nf.GetRightHandMat(model.model.GetModelLong(), model.entityData.rightHandMat);
        
        matrixStack.pushPose();
        matrixStack.last().pose().mul(convertToMatrix4f(nf, model.entityData.rightHandMat));
        
        // 基础旋转：剑朝前（原始状态朝上，绕X轴旋转90度使其朝前）
        matrixStack.mulPose(new Quaternionf().rotateX(90.0f * ((float)Math.PI / 180F)));
        // 翻转物品（修正弓等物品反向问题）
        matrixStack.mulPose(new Quaternionf().rotateY(180.0f * ((float)Math.PI / 180F)));
        
        // 可配置的额外旋转
        float rotationX = getItemRotation(player, model, InteractionHand.MAIN_HAND, "x");
        matrixStack.mulPose(new Quaternionf().rotateX(rotationX * ((float)Math.PI / 180F)));
        
        float rotationY = getItemRotation(player, model, InteractionHand.MAIN_HAND, "y");
        matrixStack.mulPose(new Quaternionf().rotateY(rotationY * ((float)Math.PI / 180F)));
        
        float rotationZ = getItemRotation(player, model, InteractionHand.MAIN_HAND, "z");
        matrixStack.mulPose(new Quaternionf().rotateZ(rotationZ * ((float)Math.PI / 180F)));
        
        matrixStack.scale(10.0f, 10.0f, 10.0f);
        
        Minecraft.getInstance().getItemRenderer().renderStatic(
            player, player.getMainHandItem(), ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, 
            matrixStack, vertexConsumers, player.level(), packedLight, OverlayTexture.NO_OVERLAY, 0
        );
        
        matrixStack.popPose();
    }
    
    private static void renderOffHandItem(AbstractClientPlayer player, ModelWithEntityData model,
                                          PoseStack matrixStack, MultiBufferSource vertexConsumers, int packedLight) {
        NativeFunc nf = NativeFunc.GetInst();
        nf.GetLeftHandMat(model.model.GetModelLong(), model.entityData.leftHandMat);
        
        matrixStack.pushPose();
        matrixStack.last().pose().mul(convertToMatrix4f(nf, model.entityData.leftHandMat));
        
        // 基础旋转：剑朝前（原始状态朝上，绕X轴旋转90度使其朝前）
        matrixStack.mulPose(new Quaternionf().rotateX(90.0f * ((float)Math.PI / 180F)));
        // 翻转物品（修正弓等物品反向问题）
        matrixStack.mulPose(new Quaternionf().rotateY(180.0f * ((float)Math.PI / 180F)));
        
        // 可配置的额外旋转
        float rotationX = getItemRotation(player, model, InteractionHand.OFF_HAND, "x");
        matrixStack.mulPose(new Quaternionf().rotateX(rotationX * ((float)Math.PI / 180F)));
        
        float rotationY = getItemRotation(player, model, InteractionHand.OFF_HAND, "y");
        matrixStack.mulPose(new Quaternionf().rotateY(rotationY * ((float)Math.PI / 180F)));
        
        float rotationZ = getItemRotation(player, model, InteractionHand.OFF_HAND, "z");
        matrixStack.mulPose(new Quaternionf().rotateZ(rotationZ * ((float)Math.PI / 180F)));
        
        matrixStack.scale(10.0f, 10.0f, 10.0f);
        
        Minecraft.getInstance().getItemRenderer().renderStatic(
            player, player.getOffhandItem(), ItemDisplayContext.THIRD_PERSON_LEFT_HAND, true, 
            matrixStack, vertexConsumers, player.level(), packedLight, OverlayTexture.NO_OVERLAY, 0
        );
        
        matrixStack.popPose();
    }
    
    private static float getItemRotation(AbstractClientPlayer player, ModelWithEntityData model, 
                                        InteractionHand hand, String axis) {
        String itemId = getItemId(player, hand);
        String handStr = (hand == InteractionHand.MAIN_HAND) ? "Right" : "Left";
        String handState = getHandState(player, hand);
        
        String specificKey = itemId + "_" + handStr + "_" + handState + "_" + axis;
        if (model.properties.getProperty(specificKey) != null) {
            return Float.parseFloat(model.properties.getProperty(specificKey));
        }
        
        String defaultKey = "default_" + axis;
        if (model.properties.getProperty(defaultKey) != null) {
            return Float.parseFloat(model.properties.getProperty(defaultKey));
        }
        
        return 0.0f;
    }
    
    private static String getHandState(AbstractClientPlayer player, InteractionHand hand) {
        if (hand == player.getUsedItemHand() && player.isUsingItem()) {
            return "using";
        } else if (hand == player.swingingArm && player.swinging) {
            return "swinging";
        }
        return "idle";
    }
    
    private static String getItemId(AbstractClientPlayer player, InteractionHand hand) {
        String descriptionId = player.getItemInHand(hand).getItem().getDescriptionId();
        return descriptionId.substring(descriptionId.indexOf(".") + 1);
    }
    
    private static Matrix4f convertToMatrix4f(NativeFunc nf, long data) {
        Matrix4f result = new Matrix4f(
            readFloat(nf, data, 0),  readFloat(nf, data, 16), readFloat(nf, data, 32), readFloat(nf, data, 48),
            readFloat(nf, data, 4),  readFloat(nf, data, 20), readFloat(nf, data, 36), readFloat(nf, data, 52),
            readFloat(nf, data, 8),  readFloat(nf, data, 24), readFloat(nf, data, 40), readFloat(nf, data, 56),
            readFloat(nf, data, 12), readFloat(nf, data, 28), readFloat(nf, data, 44), readFloat(nf, data, 60)
        );
        result.transpose();
        return result;
    }
    
    private static float readFloat(NativeFunc nf, long data, long pos) {
        int temp = 0;
        temp |= nf.ReadByte(data, pos) & 0xff;
        temp |= (nf.ReadByte(data, pos + 1) & 0xff) << 8;
        temp |= (nf.ReadByte(data, pos + 2) & 0xff) << 16;
        temp |= (nf.ReadByte(data, pos + 3) & 0xff) << 24;
        return Float.intBitsToFloat(temp);
    }
}
