package com.shiroha.mmdskin.maid;

import com.shiroha.mmdskin.renderer.animation.MMDAnimManager;
import com.shiroha.mmdskin.renderer.core.EntityAnimState;
import com.shiroha.mmdskin.renderer.core.RenderContext;
import com.shiroha.mmdskin.renderer.model.MMDModelManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * 女仆 MMD 模型渲染器
 * 
 * 负责渲染女仆的 MMD 模型，处理动画状态转换。
 */
public class MaidMMDRenderer {
    private static final Logger logger = LogManager.getLogger();
    
    /**
     * 渲染女仆的 MMD 模型
     * 
     * @param entity 女仆实体
     * @param maidUUID 女仆 UUID
     * @param entityYaw 实体偏航角
     * @param partialTicks 插值时间
     * @param poseStack 变换矩阵栈
     * @param packedLight 光照值
     * @return 是否成功渲染（如果返回 false，应使用原版渲染）
     */
    public static boolean render(LivingEntity entity, UUID maidUUID, float entityYaw, 
                                  float partialTicks, PoseStack poseStack, int packedLight) {
        // 检查是否有绑定的 MMD 模型
        if (!MaidMMDModelManager.hasMMDModel(maidUUID)) {
            return false;
        }
        
        // 获取模型
        MMDModelManager.Model modelData = MaidMMDModelManager.getModel(maidUUID);
        if (modelData == null || modelData.model == null) {
            return false;
        }
        
        try {
            // 加载模型属性
            modelData.loadModelProperties(false);
            
            // 更新动画状态
            updateAnimationState(entity, modelData);
            
            // 计算渲染偏移
            Vector3f entityTrans = getEntityTranslation(modelData);
            
            // 女仆实体不使用 pitch 旋转整个模型（避免移动时倾斜）
            // 只使用 yaw 控制朝向，pitch 保持 0
            float entityPitch = 0.0f;
            
            // 应用模型缩放
            float modelSize = getModelSize(modelData);
            poseStack.scale(modelSize, modelSize, modelSize);
            
            // 设置正确的 shader（避免使用 Orihime BedrockModel 留下的 shader 导致 flat shading）
            RenderSystem.setShader(GameRenderer::getRendertypeEntityTranslucentShader);
            
            // 渲染模型
            modelData.model.render(entity, entityYaw, entityPitch, entityTrans, partialTicks, poseStack, packedLight, RenderContext.WORLD);
            
            return true;
        } catch (Exception e) {
            logger.error("渲染女仆 MMD 模型失败: {}", maidUUID, e);
            return false;
        }
    }
    
    /**
     * 更新动画状态（针对女仆实体的简化版本）
     */
    private static void updateAnimationState(LivingEntity entity, MMDModelManager.Model modelData) {
        if (modelData.entityData == null) {
            return;
        }
        
        EntityAnimState entityData = modelData.entityData;
        
        // 简化的动画状态判断（针对女仆）
        EntityAnimState.State targetState;
        
        if (entity.getHealth() <= 0) {
            targetState = EntityAnimState.State.Die;
        } else if (entity.isSleeping()) {
            targetState = EntityAnimState.State.Sleep;
        } else if (entity.isPassenger()) {
            targetState = EntityAnimState.State.Ride;
        } else if (entity.isSwimming()) {
            targetState = EntityAnimState.State.Swim;
        } else if (entity.onClimbable()) {
            targetState = EntityAnimState.State.OnClimbable;
        } else if (entity.isSprinting()) {
            targetState = EntityAnimState.State.Sprint;
        } else if (hasMovement(entity)) {
            targetState = EntityAnimState.State.Walk;
        } else {
            targetState = EntityAnimState.State.Idle;
        }
        
        // 只在状态变化时切换动画
        if (entityData.stateLayers[0] != targetState) {
            entityData.stateLayers[0] = targetState;
            String animName = EntityAnimState.getPropertyName(targetState);
            modelData.model.changeAnim(MMDAnimManager.GetAnimModel(modelData.model, animName), 0);
        }
    }
    
    private static boolean hasMovement(LivingEntity entity) {
        return entity.getX() - entity.xo != 0.0 || entity.getZ() - entity.zo != 0.0;
    }
    
    /**
     * 获取实体渲染偏移
     */
    private static Vector3f getEntityTranslation(MMDModelManager.Model modelData) {
        float x = 0.0f;
        float y = 0.0f;
        float z = 0.0f;
        
        // 从模型属性读取偏移
        if (modelData.properties != null) {
            String transStr = modelData.properties.getProperty("entityTrans");
            if (transStr != null) {
                String[] parts = transStr.split(",");
                if (parts.length == 3) {
                    try {
                        x = Float.parseFloat(parts[0].trim());
                        y = Float.parseFloat(parts[1].trim());
                        z = Float.parseFloat(parts[2].trim());
                    } catch (NumberFormatException e) {
                        // 使用默认值
                    }
                }
            }
        }
        
        return new Vector3f(x, y, z);
    }
    
    /**
     * 获取模型缩放大小
     */
    private static float getModelSize(MMDModelManager.Model modelData) {
        if (modelData.properties != null) {
            String value = modelData.properties.getProperty("size");
            if (value != null) {
                try {
                    return Float.parseFloat(value);
                } catch (NumberFormatException e) {
                    // 使用默认值
                }
            }
        }
        return 1.0f;
    }
    
    /**
     * 设置实体速度（用于惯性效果）
     */
    public static void setEntityVelocity(UUID maidUUID, float vx, float vy, float vz) {
        MMDModelManager.Model modelData = MaidMMDModelManager.getModel(maidUUID);
        if (modelData != null && modelData.model != null) {
            // 调用物理系统设置速度
            // 这需要通过 JNI 调用，暂时留空
        }
    }
}
