package com.shiroha.mmdskin.config;

/**
 * VR 联动配置子接口（ISP 原则）
 */
public interface IVRConfig {

    /** VR 联动是否启用 */
    default boolean isVREnabled() { return true; }

    /** 手臂 IK 强度 (0.0~1.0) */
    default float getVRArmIKStrength() { return 1.0f; }
}
