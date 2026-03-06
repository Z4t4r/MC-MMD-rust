package com.shiroha.mmdskin.renderer.model;

/**
 * MMD 模型材质（统一定义，避免多个渲染器重复定义）
 */
public class MMDMaterial {
    public int tex = 0;
    public boolean hasAlpha = false;
    /** 是否由本渲染器创建（dispose 时需要删除） */
    public boolean ownsTexture = false;
}
