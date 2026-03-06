package com.shiroha.mmdskin.config;

/**
 * Toon 渲染配置子接口
 * 包含色阶、描边、边缘光、阴影色、高光等 Toon 着色参数
 */
public interface IToonConfig {
    /** Toon 渲染启用状态（默认开启） */
    default boolean isToonRenderingEnabled() { return true; }

    /** Toon 色阶数量（默认3） */
    default int getToonLevels() { return 3; }

    /** Toon 描边启用状态（默认关闭） */
    default boolean isToonOutlineEnabled() { return false; }

    /** Toon 描边宽度（默认0.003） */
    default float getToonOutlineWidth() { return 0.003f; }

    // 边缘光
    default float getToonRimPower() { return 5.0f; }
    default float getToonRimIntensity() { return 0.1f; }

    // 阴影色
    default float getToonShadowR() { return 0.8f; }
    default float getToonShadowG() { return 0.8f; }
    default float getToonShadowB() { return 0.8f; }

    // 高光
    default float getToonSpecularPower() { return 30.0f; }
    default float getToonSpecularIntensity() { return 0.08f; }

    // 描边颜色
    default float getToonOutlineR() { return 0.0f; }
    default float getToonOutlineG() { return 0.0f; }
    default float getToonOutlineB() { return 0.0f; }
}
