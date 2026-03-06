package com.shiroha.mmdskin.renderer.model;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;

/**
 * 光照计算工具类
 * 封装光照采样逻辑，避免渲染器直接穿透 Entity → Level → LightEngine 调用链
 */
public final class LightingHelper {

    private LightingHelper() {}

    /**
     * 光照数据
     * @param blockLight 方块光照 (0-15)
     * @param skyLight 天空光照 (0-15)
     * @param skyDarken 天空亮度衰减
     * @param intensity 综合光照强度 (0.1 ~ 1.0)
     */
    public record LightData(int blockLight, int skyLight, float skyDarken, float intensity) {}

    /** 默认光照（世界未加载时的安全回退值） */
    private static final LightData DEFAULT_LIGHT = new LightData(0, 15, 0, 1.0f);

    public static LightData sampleLight(Entity entity, Minecraft mc) {
        if (mc.level == null) return DEFAULT_LIGHT;
        mc.level.updateSkyBrightness();
        int eyeHeight = (int) (entity.getEyeY() - entity.getBlockY());
        int blockLight = entity.level().getBrightness(LightLayer.BLOCK, entity.blockPosition().above(eyeHeight));
        int skyLight = entity.level().getBrightness(LightLayer.SKY, entity.blockPosition().above(eyeHeight));
        float skyDarken = mc.level.getSkyDarken();

        float blockLightFactor = blockLight / 15.0f;
        float skyLightFactor = (skyLight / 15.0f) * ((15.0f - skyDarken) / 15.0f);
        float lightIntensity = Math.max(blockLightFactor, skyLightFactor);
        // 最低亮度阈值，防止完全黑暗
        lightIntensity = 0.1f + lightIntensity * 0.9f;

        return new LightData(blockLight, skyLight, skyDarken, lightIntensity);
    }
}
