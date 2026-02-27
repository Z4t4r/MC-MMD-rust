package com.shiroha.mmdskin.config;

/**
 * 配置提供者基类（DRY 原则）
 * 将 IConfigProvider 的所有委托方法集中实现，
 * Fabric/Forge 子类只需提供 configPath 即可。
 */
public abstract class AbstractMmdSkinConfig implements ConfigManager.IConfigProvider {
    protected ConfigData data;

    protected AbstractMmdSkinConfig(ConfigData data) {
        this.data = data;
    }

    // ==================== 渲染配置 ====================

    @Override public boolean isOpenGLLightingEnabled() { return data.openGLEnableLighting; }
    @Override public int getModelPoolMaxCount() { return data.modelPoolMaxCount; }
    @Override public boolean isMMDShaderEnabled() { return data.mmdShaderEnabled; }
    @Override public boolean isGpuSkinningEnabled() { return data.gpuSkinningEnabled; }
    @Override public boolean isGpuMorphEnabled() { return data.gpuMorphEnabled; }
    @Override public int getMaxBones() { return data.maxBones; }

    // ==================== Toon 渲染 ====================

    @Override public boolean isToonRenderingEnabled() { return data.toonRenderingEnabled; }
    @Override public int getToonLevels() { return data.toonLevels; }
    @Override public boolean isToonOutlineEnabled() { return data.toonOutlineEnabled; }
    @Override public float getToonOutlineWidth() { return data.toonOutlineWidth; }
    @Override public float getToonRimPower() { return data.toonRimPower; }
    @Override public float getToonRimIntensity() { return data.toonRimIntensity; }
    @Override public float getToonShadowR() { return data.toonShadowR; }
    @Override public float getToonShadowG() { return data.toonShadowG; }
    @Override public float getToonShadowB() { return data.toonShadowB; }
    @Override public float getToonSpecularPower() { return data.toonSpecularPower; }
    @Override public float getToonSpecularIntensity() { return data.toonSpecularIntensity; }
    @Override public float getToonOutlineR() { return data.toonOutlineR; }
    @Override public float getToonOutlineG() { return data.toonOutlineG; }
    @Override public float getToonOutlineB() { return data.toonOutlineB; }

    // ==================== 物理引擎（Bullet3） ====================

    @Override public float getPhysicsGravityY() { return data.physicsGravityY; }
    @Override public float getPhysicsFps() { return data.physicsFps; }
    @Override public int getPhysicsMaxSubstepCount() { return data.physicsMaxSubstepCount; }
    @Override public float getPhysicsInertiaStrength() { return data.physicsInertiaStrength; }
    @Override public float getPhysicsMaxLinearVelocity() { return data.physicsMaxLinearVelocity; }
    @Override public float getPhysicsMaxAngularVelocity() { return data.physicsMaxAngularVelocity; }
    @Override public boolean isPhysicsJointsEnabled() { return data.physicsJointsEnabled; }
    @Override public boolean isPhysicsKinematicFilter() { return data.physicsKinematicFilter; }
    @Override public boolean isPhysicsDebugLog() { return data.physicsDebugLog; }

    // ==================== 第一人称 / 调试 / 纹理缓存 ====================

    @Override public boolean isFirstPersonModelEnabled() { return data.firstPersonModelEnabled; }
    @Override public float getFirstPersonCameraForwardOffset() { return data.firstPersonCameraForwardOffset; }
    @Override public float getFirstPersonCameraVerticalOffset() { return data.firstPersonCameraVerticalOffset; }
    @Override public boolean isDebugHudEnabled() { return data.debugHudEnabled; }
    @Override public int getTextureCacheBudgetMB() { return data.textureCacheBudgetMB; }

    // ==================== VR 联动 ====================

    @Override public boolean isVREnabled() { return data.vrEnabled; }
    @Override public float getVRArmIKStrength() { return data.vrArmIKStrength; }
}
