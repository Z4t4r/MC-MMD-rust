package com.shiroha.skinlayers3d.renderer.shader;

/**
 * CPU 蒙皮版本的 Toon 着色器
 * 
 * 继承 ToonShaderBase，只需提供不含骨骼蒙皮的顶点着色器。
 * 蒙皮已在 Rust 引擎完成，此着色器直接接收已蒙皮的顶点位置和法线。
 * 
 * 用于 MMDModelOpenGL（CPU 蒙皮模式）
 */
public class ToonShaderCpu extends ToonShaderBase {
    
    // ==================== CPU 蒙皮版顶点着色器（无骨骼计算） ====================
    
    private static final String MAIN_VERTEX_SHADER = """
        #version 330 core
        
        layout(location = 0) in vec3 Position;
        layout(location = 1) in vec3 Normal;
        layout(location = 2) in vec2 UV0;
        
        uniform mat4 ProjMat;
        uniform mat4 ModelViewMat;
        
        out vec2 texCoord0;
        out vec3 viewNormal;
        out vec3 viewPos;
        
        void main() {
            // Position 和 Normal 已经是蒙皮后的数据（由 Rust 引擎计算）
            vec4 viewPosition = ModelViewMat * vec4(Position, 1.0);
            
            mat3 normalMatrix = mat3(ModelViewMat);
            vec3 transformedNormal = normalMatrix * Normal;
            
            gl_Position = ProjMat * viewPosition;
            texCoord0 = UV0;
            viewNormal = normalize(transformedNormal);
            viewPos = viewPosition.xyz;
        }
        """;
    
    private static final String OUTLINE_VERTEX_SHADER = """
        #version 330 core
        
        layout(location = 0) in vec3 Position;
        layout(location = 1) in vec3 Normal;
        
        uniform mat4 ProjMat;
        uniform mat4 ModelViewMat;
        uniform float OutlineWidth;
        
        out vec3 viewNormal;
        out vec3 viewPos;
        
        void main() {
            // Position 和 Normal 已经是蒙皮后的数据
            mat3 normalMatrix = mat3(ModelViewMat);
            vec3 transformedNormal = normalize(normalMatrix * Normal);
            
            // 沿法线方向扩张顶点（背面扩张法）
            vec4 vPos = ModelViewMat * vec4(Position, 1.0);
            vPos.xyz += transformedNormal * OutlineWidth;
            
            gl_Position = ProjMat * vPos;
            viewNormal = transformedNormal;
            viewPos = vPos.xyz;
        }
        """;
    
    // ==================== 实现抽象方法 ====================
    
    @Override
    protected String getMainVertexShader() {
        return MAIN_VERTEX_SHADER;
    }
    
    @Override
    protected String getOutlineVertexShader() {
        return OUTLINE_VERTEX_SHADER;
    }
    
    @Override
    protected void onInitialized() {
        // CPU 版本无额外初始化
    }
    
    @Override
    protected String getShaderName() {
        return "ToonShaderCpu";
    }
}
