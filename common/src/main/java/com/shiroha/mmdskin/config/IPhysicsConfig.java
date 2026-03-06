package com.shiroha.mmdskin.config;

/**
 * 物理引擎配置子接口（Bullet3）
 *
 * Bullet3 内置处理阻尼、约束求解等，这里只保留可调节的高级参数。
 * 默认值与 Rust PhysicsConfig 保持一致。
 */
public interface IPhysicsConfig {

    /** 是否启用物理模拟（默认 true） */
    default boolean isPhysicsEnabled() { return true; }

    /** 重力 Y 分量（负数向下），默认 -98.0（MMD 标准） */
    default float getPhysicsGravityY() { return -98.0f; }

    /** 物理 FPS（Bullet3 固定时间步），默认 60 */
    default float getPhysicsFps() { return 60.0f; }

    /** 每帧最大子步数，默认 5 */
    default int getPhysicsMaxSubstepCount() { return 5; }

    /** 惯性效果强度（0.0=无惯性, 1.0=正常），默认 1.0 */
    default float getPhysicsInertiaStrength() { return 0.5f; }

    /** 最大线速度（防止物理爆炸），默认 20.0 */
    default float getPhysicsMaxLinearVelocity() { return 20.0f; }

    /** 最大角速度（防止物理爆炸），默认 20.0 */
    default float getPhysicsMaxAngularVelocity() { return 20.0f; }

    /** 是否启用关节（默认 true） */
    default boolean isPhysicsJointsEnabled() { return true; }

    /** 是否输出调试日志（默认 false） */
    default boolean isPhysicsDebugLog() { return false; }
}
