package com.shiroha.mmdskin.renderer.core;

import com.shiroha.mmdskin.renderer.model.ModelInfo;

/**
 * MMD 模型工厂接口 (DIP - 依赖倒置原则)
 * 
 * core 层通过此接口创建模型，而不直接依赖具体实现类。
 * 具体的工厂实现在 model 层注册。
 */
public interface IMMDModelFactory {
    
    /**
     * 获取渲染模式分类（替代字符串匹配，OCP）
     */
    RenderCategory getCategory();
    
    /**
     * 获取工厂支持的渲染模式名称（用于日志和 UI 显示）
     */
    String getModeName();
    
    /**
     * 获取工厂优先级（数值越大优先级越高）
     */
    int getPriority();
    
    /**
     * 检查此工厂是否可用（如 GPU 蒙皮需要检查 OpenGL 版本）
     */
    boolean isAvailable();
    
    /**
     * 是否支持 PMD 格式模型
     */
    default boolean supportsPMD() {
        return true;
    }
    
    /**
     * 创建模型实例
     * 
     * @param modelFilename 模型文件路径
     * @param modelDir 模型目录
     * @param isPMD 是否为 PMD 格式
     * @param layerCount 动画层数
     * @return 创建的模型实例，失败返回 null
     */
    IMMDModel createModel(String modelFilename, String modelDir, boolean isPMD, long layerCount);
    
    /**
     * 从已加载的模型句柄创建渲染实例（Phase 2：GL 资源创建，必须在渲染线程调用）
     * 
     * @param modelHandle 后台线程加载的模型句柄
     * @param modelDir 模型目录
     * @return 创建的模型实例，失败返回 null
     */
    IMMDModel createModelFromHandle(long modelHandle, String modelDir);
    
    /**
     * 根据 ModelInfo 创建模型（便捷方法）
     */
    default IMMDModel createModel(ModelInfo modelInfo, long layerCount) {
        if (modelInfo == null) {
            return null;
        }
        return createModel(
            modelInfo.getModelFilePath(),
            modelInfo.getFolderPath(),
            modelInfo.isPMD(),
            layerCount
        );
    }
}
