package com.shiroha.mmdskin.renderer.shader;

import org.lwjgl.opengl.GL46C;

/**
 * SSBO 绑定状态保存/恢复工具
 * 
 * 在执行 Compute Shader 或自定义渲染时，需要保存当前所有 SSBO 绑定状态，
 * 完成后恢复原状，避免破坏光影 mod（如 Eclipse Shaders）的 SSBO 绑定。
 * 
 * 用法：
 *   var bindings = new SSBOBindings();   // 构造时自动保存当前状态
 *   // ... 执行 Compute Shader / 绑定自定义 SSBO ...
 *   bindings.restore();                  // 恢复之前的状态
 * 
 * 感谢 AR 提供此方案。
 */
public class SSBOBindings {
    
    // 延迟初始化，避免类加载时无 GL 上下文导致异常
    private static volatile int maxBindings = -1;
    
    public static int getMaxBindings() {
        int val = maxBindings;
        if (val < 0) {
            val = GL46C.glGetInteger(GL46C.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS);
            maxBindings = val;
        }
        return val;
    }
    
    private final int[]  bufferHandles;
    private final long[] bufferBindingOffsets;
    private final long[] bufferBindingSizes;
    
    /**
     * 构造时自动记录当前所有 SSBO 绑定点的状态
     */
    public SSBOBindings() {
        int bindings = getMaxBindings();
        this.bufferHandles        = new int [bindings];
        this.bufferBindingOffsets  = new long[bindings];
        this.bufferBindingSizes    = new long[bindings];
        
        for (var bindingPoint = 0; bindingPoint < bindings; bindingPoint++) {
            this.bufferHandles       [bindingPoint] = GL46C.glGetIntegeri(GL46C.GL_SHADER_STORAGE_BUFFER_BINDING, bindingPoint);
            this.bufferBindingOffsets [bindingPoint] = GL46C.glGetInteger64i(GL46C.GL_SHADER_STORAGE_BUFFER_START, bindingPoint);
            this.bufferBindingSizes   [bindingPoint] = GL46C.glGetInteger64i(GL46C.GL_SHADER_STORAGE_BUFFER_SIZE, bindingPoint);
        }
    }
    
    /**
     * 恢复之前保存的所有 SSBO 绑定状态
     */
    public void restore() {
        for (var bindingPoint = 0; bindingPoint < bufferHandles.length; bindingPoint++) {
            var bufferHandle        = bufferHandles       [bindingPoint];
            var bufferBindingOffset = bufferBindingOffsets [bindingPoint];
            var bufferBindingSize   = bufferBindingSizes   [bindingPoint];
            
            // 跳过已被删除的 buffer handle（光影 mod 可能在管线重建时删除旧 buffer，
            // AMD 驱动对绑定无效 handle 可能触发 EXCEPTION_ACCESS_VIOLATION）
            if (bufferHandle != 0 && !GL46C.glIsBuffer(bufferHandle)) {
                GL46C.glBindBufferBase(
                        GL46C.GL_SHADER_STORAGE_BUFFER,
                        bindingPoint,
                        0
                );
                continue;
            }
            
            if (        bufferBindingOffset == 0
                    &&  bufferBindingSize   == 0
            ) {
                GL46C.glBindBufferBase(
                        GL46C.GL_SHADER_STORAGE_BUFFER,
                        bindingPoint,
                        bufferHandle
                );
            } else {
                GL46C.glBindBufferRange(
                        GL46C.GL_SHADER_STORAGE_BUFFER,
                        bindingPoint,
                        bufferHandle,
                        bufferBindingOffset,
                        bufferBindingSize
                );
            }
        }
    }
}
