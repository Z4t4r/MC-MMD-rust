package com.shiroha.mmdskin.renderer.core;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/**
 * MMD 模型接口 (ISP - 接口隔离原则)
 * 
 * 定义 MMD 模型的核心操作，所有模型实现类必须实现此接口。
 */
public interface IMMDModel {
    
    /**
     * 渲染模型
     * 
     * @param entityIn 实体
     * @param entityYaw 实体偏航角
     * @param entityPitch 实体俯仰角
     * @param entityTrans 实体位移
     * @param tickDelta 插值因子
     * @param mat 变换矩阵栈
     * @param packedLight 打包光照值
     * @param context 渲染上下文
     */
    void render(Entity entityIn, float entityYaw, float entityPitch, 
            Vector3f entityTrans, float tickDelta, PoseStack mat, int packedLight,
            RenderContext context);

    /**
     * 切换动画
     * 
     * @param anim 动画句柄
     * @param layer 动画层
     */
    void ChangeAnim(long anim, long layer);
    
    /**
     * 带过渡地切换动画
     * 
     * @param anim 动画句柄
     * @param layer 动画层
     * @param transitionTime 过渡时间（秒）
     */
    void TransitionAnim(long anim, long layer, float transitionTime);

    /**
     * 重置物理模拟
     */
    void ResetPhysics();

    /**
     * 获取模型本地句柄
     */
    long GetModelLong();

    /**
     * 获取模型目录路径
     */
    String GetModelDir();
    
    /**
     * 获取模型名称（文件夹名）
     * 默认实现从 modelDir 路径中提取最后一段目录名
     */
    default String getModelName() {
        String dir = GetModelDir();
        if (dir == null || dir.isEmpty()) return "";
        // 兼容 / 和 \ 分隔符
        dir = dir.replace('\\', '/');
        if (dir.endsWith("/")) dir = dir.substring(0, dir.length() - 1);
        int lastSlash = dir.lastIndexOf('/');
        return lastSlash >= 0 ? dir.substring(lastSlash + 1) : dir;
    }
    
    /**
     * 释放模型资源（OpenGL 缓冲区、本地内存等）
     * 实现类应在此方法中清理所有资源
     */
    void dispose();
}
