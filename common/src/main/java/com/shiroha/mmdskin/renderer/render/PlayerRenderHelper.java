package com.shiroha.mmdskin.renderer.render;

import com.shiroha.mmdskin.MmdSkinClient;
import com.shiroha.mmdskin.renderer.core.RenderParams;
import com.shiroha.mmdskin.renderer.model.MMDModelManager.Model;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * 玩家渲染辅助工具类（提取自 Fabric/Forge Mixin 的公共逻辑）
 * 消除两个平台 Mixin 中的重复代码
 */
public final class PlayerRenderHelper {
    
    private PlayerRenderHelper() {}
    
    /**
     * 计算渲染参数（体朝向、俯仰角、平移偏移）
     */
    public static RenderParams calculateRenderParams(AbstractClientPlayer player, Model modelData, float tickDelta) {
        RenderParams params = new RenderParams();
        params.bodyYaw = Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot);
        params.bodyPitch = 0.0f;
        params.translation = new Vector3f(0.0f);
        
        if (player.isFallFlying()) {
            params.bodyPitch = player.getXRot() + getPropertyFloat(modelData, "flyingPitch", 0.0f);
            params.translation = getPropertyVector(modelData, "flyingTrans");
        } else if (player.isSleeping()) {
            params.bodyYaw = player.getBedOrientation().toYRot() + 180.0f;
            params.bodyPitch = getPropertyFloat(modelData, "sleepingPitch", 0.0f);
            params.translation = getPropertyVector(modelData, "sleepingTrans");
        } else if (player.isSwimming()) {
            params.bodyPitch = player.getXRot() + getPropertyFloat(modelData, "swimmingPitch", 0.0f);
            params.translation = getPropertyVector(modelData, "swimmingTrans");
        } else if (player.isVisuallyCrawling()) {
            params.bodyPitch = getPropertyFloat(modelData, "crawlingPitch", 0.0f);
            params.translation = getPropertyVector(modelData, "crawlingTrans");
        }
        
        return params;
    }
    
    /**
     * 获取模型尺寸 [worldScale, inventoryScale]
     */
    public static float[] getModelSize(Model modelData) {
        float[] size = new float[2];
        size[0] = getPropertyFloat(modelData, "size", 1.0f);
        size[1] = getPropertyFloat(modelData, "size_in_inventory", 1.0f);
        return size;
    }
    
    /**
     * 安全获取属性浮点值
     */
    public static float getPropertyFloat(Model modelData, String key, float defaultValue) {
        String value = modelData.properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 获取属性向量值
     */
    public static Vector3f getPropertyVector(Model modelData, String key) {
        String value = modelData.properties.getProperty(key);
        return value == null ? new Vector3f(0.0f) : MmdSkinClient.str2Vec3f(value);
    }
}
