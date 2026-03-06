package com.shiroha.mmdskin.renderer.model.factory;

import com.shiroha.mmdskin.NativeFunc;
import com.shiroha.mmdskin.renderer.core.IMMDModel;
import com.shiroha.mmdskin.renderer.core.IMMDModelFactory;
import com.shiroha.mmdskin.renderer.core.RenderCategory;
import com.shiroha.mmdskin.renderer.model.MMDModelGpuSkinning;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * GPU 蒙皮模型工厂
 * 
 * 使用 GPU 进行蒙皮计算，大幅提升大面数模型性能。
 * 需要 OpenGL 4.3+ 支持（SSBO）。
 */
public class GpuSkinningModelFactory implements IMMDModelFactory {
    private static final Logger logger = LogManager.getLogger();
    
    /** 优先级：中等 */
    private static final int PRIORITY = 10;
    
    @Override
    public RenderCategory getCategory() {
        return RenderCategory.GPU_SKINNING;
    }
    
    @Override
    public String getModeName() {
        return "GPU蒙皮";
    }
    
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    @Override
    public boolean isAvailable() {
        // Android GL 翻译层（gl4es/ANGLE）不支持 OpenGL 4.3 Compute Shader
        if (NativeFunc.isAndroid()) return false;
        // 桌面端不做前置版本检查，让着色器初始化时自己检测
        return true;
    }
    
    @Override
    public IMMDModel createModel(String modelFilename, String modelDir, boolean isPMD, long layerCount) {
        if (!isAvailable()) {
            logger.warn("GPU 蒙皮不可用，无法创建模型");
            return null;
        }
        
        try {
            return MMDModelGpuSkinning.Create(modelFilename, modelDir, isPMD, layerCount);
        } catch (Exception e) {
            logger.error("GPU 蒙皮模型创建失败: {}", modelFilename, e);
            return null;
        }
    }
    
    @Override
    public IMMDModel createModelFromHandle(long modelHandle, String modelDir) {
        try {
            return MMDModelGpuSkinning.createFromHandle(modelHandle, modelDir);
        } catch (Exception e) {
            logger.error("GPU 蒙皮模型（从句柄）创建失败", e);
            return null;
        }
    }
}
