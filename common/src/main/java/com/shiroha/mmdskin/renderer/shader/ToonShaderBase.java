package com.shiroha.mmdskin.renderer.shader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL46C;

import java.nio.FloatBuffer;

/**
 * Toon 着色器抽象基类
 * 
 * 提取 GPU 蒙皮版本（ToonShader）和 CPU 蒙皮版本（ToonShaderCpu）的公共逻辑：
 * - 共享的片段着色器代码
 * - 共享的 uniform 设置方法
 * - 共享的着色器编译辅助方法
 * 
 * 子类负责提供各自的顶点着色器（蒙皮方式不同）
 */
public abstract class ToonShaderBase {
    protected static final Logger logger = LogManager.getLogger();
    
    // 着色器程序
    protected int mainProgram = 0;
    protected int outlineProgram = 0;
    protected boolean initialized = false;
    
    // ==================== 共享的片段着色器逻辑 ====================
    
    /**
     * 获取 GLSL 版本声明，子类可覆盖
     * GPU 版本需要 460 来支持 SSBO，CPU 版本用 330 即可
     */
    protected String getGlslVersion() {
        return "#version 330 core";
    }
    
    /**
     * 主着色器片段着色器核心逻辑（不含版本声明）
     */
    protected static final String MAIN_FRAGMENT_SHADER_BODY = """
        
        in vec2 texCoord0;
        in vec3 viewNormal;
        in vec3 viewPos;
        
        uniform sampler2D Sampler0;
        uniform float LightIntensity;
        uniform int ToonLevels;          // 色阶数量（2-5）
        uniform float RimPower;          // 边缘光锐度
        uniform float RimIntensity;      // 边缘光强度
        uniform vec3 ShadowColor;        // 阴影色调
        uniform float SpecularPower;     // 高光锐度
        uniform float SpecularIntensity; // 高光强度
        
        // MRT 多输出：兼容 Iris G-buffer FBO
        // location 0 → colortex0（漫反射色 + alpha）
        // location 1 → colortex1（编码法线）
        // location 2 → colortex2（光照图 / 高光数据）
        // location 3 → colortex3（保留）
        layout(location = 0) out vec4 fragColor;
        layout(location = 1) out vec4 fragData1;
        layout(location = 2) out vec4 fragData2;
        layout(location = 3) out vec4 fragData3;
        
        // 色阶化函数
        float toonify(float value, int levels) {
            return floor(value * float(levels) + 0.5) / float(levels);
        }
        
        void main() {
            vec4 texColor = texture(Sampler0, texCoord0);
            vec3 normal = normalize(viewNormal);
            
            // 主光源方向（视图空间）
            vec3 lightDir = normalize(vec3(0.2, 1.0, -0.7));
            vec3 viewDir = normalize(-viewPos);
            
            // === 漫反射（色阶化） ===
            float NdotL = dot(normal, lightDir);
            float diffuse = max(NdotL, 0.0);
            float toonDiffuse = toonify(diffuse, ToonLevels);
            
            // === 高光（色阶化） ===
            vec3 halfDir = normalize(lightDir + viewDir);
            float NdotH = max(dot(normal, halfDir), 0.0);
            float specular = pow(NdotH, SpecularPower);
            // 卡通高光：硬边界
            float toonSpecular = step(0.5, specular) * SpecularIntensity;
            
            // === 边缘光（Rim Light） ===
            float rim = 1.0 - max(dot(viewDir, normal), 0.0);
            rim = pow(rim, RimPower) * RimIntensity;
            
            // === 阴影混合 ===
            // 亮部使用原色，暗部混合阴影色
            vec3 litColor = texColor.rgb;
            vec3 shadowedColor = texColor.rgb * ShadowColor;
            vec3 baseColor = mix(shadowedColor, litColor, toonDiffuse);
            
            // === 最终合成 ===
            vec3 finalColor = baseColor * LightIntensity;
            finalColor += toonSpecular * vec3(1.0);  // 高光（白色）
            finalColor += rim * texColor.rgb;         // 边缘光
            
            // 环境光保底
            float ambient = 0.15;
            finalColor = max(finalColor, texColor.rgb * ambient);
            
            if (texColor.a < 0.004) discard;
            
            // MRT 输出
            fragColor  = vec4(finalColor, texColor.a);           // 漫反射色
            fragData1  = vec4(normal * 0.5 + 0.5, 1.0);         // 编码法线 [-1,1]→[0,1]
            fragData2  = vec4(0.0, 0.0, 0.0, 1.0);              // 光照图占位
            fragData3  = vec4(0.0, 0.0, 0.0, 1.0);              // 保留
        }
        """;
    
    /**
     * 描边片段着色器核心逻辑（不含版本声明）
     */
    protected static final String OUTLINE_FRAGMENT_SHADER_BODY = """
        
        in vec3 viewNormal;
        in vec3 viewPos;
        
        uniform vec3 OutlineColor;
        
        // MRT 多输出：兼容 Iris G-buffer FBO
        layout(location = 0) out vec4 fragColor;
        layout(location = 1) out vec4 fragData1;
        layout(location = 2) out vec4 fragData2;
        layout(location = 3) out vec4 fragData3;
        
        void main() {
            // 计算视线方向（从片段指向相机）
            vec3 viewDir = normalize(-viewPos);
            vec3 normal = normalize(viewNormal);
            
            // 法线朝向相机的区域不应该有描边（凹陷区域）
            float NdotV = dot(normal, viewDir);
            if (NdotV > 0.1) {
                discard;
            }
            
            // MRT 输出
            fragColor  = vec4(OutlineColor, 1.0);                // 描边色
            fragData1  = vec4(normal * 0.5 + 0.5, 1.0);         // 编码法线
            fragData2  = vec4(0.0, 0.0, 0.0, 1.0);              // 光照图占位
            fragData3  = vec4(0.0, 0.0, 0.0, 1.0);              // 保留
        }
        """;
    
    // ==================== 主着色器 Uniform locations ====================
    protected int projMatLocation = -1;
    protected int modelViewMatLocation = -1;
    protected int sampler0Location = -1;
    protected int lightIntensityLocation = -1;
    protected int toonLevelsLocation = -1;
    protected int rimPowerLocation = -1;
    protected int rimIntensityLocation = -1;
    protected int shadowColorLocation = -1;
    protected int specularPowerLocation = -1;
    protected int specularIntensityLocation = -1;
    
    // ==================== 描边着色器 Uniform locations ====================
    protected int outlineProjMatLocation = -1;
    protected int outlineModelViewMatLocation = -1;
    protected int outlineWidthLocation = -1;
    protected int outlineColorLocation = -1;
    
    // ==================== Attribute locations ====================
    protected int positionLocation = -1;
    protected int normalLocation = -1;
    protected int uv0Location = -1;
    protected int outlinePositionLocation = -1;
    protected int outlineNormalLocation = -1;
    
    // ==================== 抽象方法（子类实现） ====================
    
    /**
     * 获取主着色器的顶点着色器源码
     * GPU 版本包含骨骼蒙皮逻辑，CPU 版本直接使用已蒙皮的顶点
     */
    protected abstract String getMainVertexShader();
    
    /**
     * 获取描边着色器的顶点着色器源码
     */
    protected abstract String getOutlineVertexShader();
    
    /**
     * 子类初始化后的额外处理（如获取特有的 uniform/attribute locations）
     */
    protected abstract void onInitialized();
    
    /**
     * 获取着色器名称（用于日志）
     */
    protected abstract String getShaderName();
    
    // ==================== 公共初始化逻辑 ====================
    
    public boolean init() {
        if (initialized) return true;
        
        try {
            // 组装片段着色器（版本声明 + 核心逻辑）
            String mainFragShader = getGlslVersion() + "\n" + MAIN_FRAGMENT_SHADER_BODY;
            String outlineFragShader = getGlslVersion() + "\n" + OUTLINE_FRAGMENT_SHADER_BODY;
            
            // 编译主着色器
            mainProgram = compileProgram(getMainVertexShader(), mainFragShader, 
                                        getShaderName() + "主着色器");
            if (mainProgram == 0) return false;
            
            // 编译描边着色器
            outlineProgram = compileProgram(getOutlineVertexShader(), outlineFragShader, 
                                           getShaderName() + "描边着色器");
            if (outlineProgram == 0) {
                GL46C.glDeleteProgram(mainProgram);
                mainProgram = 0;
                return false;
            }
            
            // 获取公共 uniform 位置
            initCommonUniforms();
            
            // 获取公共 attribute 位置
            initCommonAttributes();
            
            // 子类额外初始化
            onInitialized();
            
            initialized = true;
            logger.info("{} 初始化成功", getShaderName());
            return true;
            
        } catch (Exception e) {
            logger.error("{} 初始化异常", getShaderName(), e);
            return false;
        }
    }
    
    private void initCommonUniforms() {
        // 主着色器 uniform
        projMatLocation = GL46C.glGetUniformLocation(mainProgram, "ProjMat");
        modelViewMatLocation = GL46C.glGetUniformLocation(mainProgram, "ModelViewMat");
        sampler0Location = GL46C.glGetUniformLocation(mainProgram, "Sampler0");
        lightIntensityLocation = GL46C.glGetUniformLocation(mainProgram, "LightIntensity");
        toonLevelsLocation = GL46C.glGetUniformLocation(mainProgram, "ToonLevels");
        rimPowerLocation = GL46C.glGetUniformLocation(mainProgram, "RimPower");
        rimIntensityLocation = GL46C.glGetUniformLocation(mainProgram, "RimIntensity");
        shadowColorLocation = GL46C.glGetUniformLocation(mainProgram, "ShadowColor");
        specularPowerLocation = GL46C.glGetUniformLocation(mainProgram, "SpecularPower");
        specularIntensityLocation = GL46C.glGetUniformLocation(mainProgram, "SpecularIntensity");
        
        // 描边着色器 uniform
        outlineProjMatLocation = GL46C.glGetUniformLocation(outlineProgram, "ProjMat");
        outlineModelViewMatLocation = GL46C.glGetUniformLocation(outlineProgram, "ModelViewMat");
        outlineWidthLocation = GL46C.glGetUniformLocation(outlineProgram, "OutlineWidth");
        outlineColorLocation = GL46C.glGetUniformLocation(outlineProgram, "OutlineColor");
    }
    
    private void initCommonAttributes() {
        // 主着色器 attribute
        positionLocation = GL46C.glGetAttribLocation(mainProgram, "Position");
        normalLocation = GL46C.glGetAttribLocation(mainProgram, "Normal");
        uv0Location = GL46C.glGetAttribLocation(mainProgram, "UV0");
        
        // 描边着色器 attribute
        outlinePositionLocation = GL46C.glGetAttribLocation(outlineProgram, "Position");
        outlineNormalLocation = GL46C.glGetAttribLocation(outlineProgram, "Normal");
    }
    
    // ==================== 着色器编译辅助方法 ====================
    
    protected int compileProgram(String vertexSource, String fragmentSource, String name) {
        int vertexShader = GL46C.glCreateShader(GL46C.GL_VERTEX_SHADER);
        GL46C.glShaderSource(vertexShader, vertexSource);
        GL46C.glCompileShader(vertexShader);
        
        if (GL46C.glGetShaderi(vertexShader, GL46C.GL_COMPILE_STATUS) == GL46C.GL_FALSE) {
            String log = GL46C.glGetShaderInfoLog(vertexShader, 8192).trim();
            logger.error("{}顶点着色器编译失败: {}", name, log);
            GL46C.glDeleteShader(vertexShader);
            return 0;
        }
        
        int fragShader = GL46C.glCreateShader(GL46C.GL_FRAGMENT_SHADER);
        GL46C.glShaderSource(fragShader, fragmentSource);
        GL46C.glCompileShader(fragShader);
        
        if (GL46C.glGetShaderi(fragShader, GL46C.GL_COMPILE_STATUS) == GL46C.GL_FALSE) {
            String log = GL46C.glGetShaderInfoLog(fragShader, 8192).trim();
            logger.error("{}片段着色器编译失败: {}", name, log);
            GL46C.glDeleteShader(vertexShader);
            GL46C.glDeleteShader(fragShader);
            return 0;
        }
        
        int program = GL46C.glCreateProgram();
        GL46C.glAttachShader(program, vertexShader);
        GL46C.glAttachShader(program, fragShader);
        GL46C.glLinkProgram(program);
        
        if (GL46C.glGetProgrami(program, GL46C.GL_LINK_STATUS) == GL46C.GL_FALSE) {
            String log = GL46C.glGetProgramInfoLog(program, 8192);
            logger.error("{}链接失败: {}", name, log);
            GL46C.glDeleteProgram(program);
            GL46C.glDeleteShader(vertexShader);
            GL46C.glDeleteShader(fragShader);
            return 0;
        }
        
        GL46C.glDeleteShader(vertexShader);
        GL46C.glDeleteShader(fragShader);
        return program;
    }
    
    // ==================== 公共 Uniform 设置方法 ====================
    
    public void useMain() {
        if (mainProgram > 0) {
            GL46C.glUseProgram(mainProgram);
        }
    }
    
    public void useOutline() {
        if (outlineProgram > 0) {
            GL46C.glUseProgram(outlineProgram);
        }
    }
    
    public void setProjectionMatrix(FloatBuffer matrix) {
        if (projMatLocation >= 0) {
            matrix.position(0);
            GL46C.glUniformMatrix4fv(projMatLocation, false, matrix);
        }
    }
    
    public void setModelViewMatrix(FloatBuffer matrix) {
        if (modelViewMatLocation >= 0) {
            matrix.position(0);
            GL46C.glUniformMatrix4fv(modelViewMatLocation, false, matrix);
        }
    }
    
    public void setSampler0(int textureUnit) {
        if (sampler0Location >= 0) {
            GL46C.glUniform1i(sampler0Location, textureUnit);
        }
    }
    
    public void setLightIntensity(float intensity) {
        if (lightIntensityLocation >= 0) {
            GL46C.glUniform1f(lightIntensityLocation, intensity);
        }
    }
    
    public void setToonLevels(int levels) {
        if (toonLevelsLocation >= 0) {
            GL46C.glUniform1i(toonLevelsLocation, Math.max(2, Math.min(5, levels)));
        }
    }
    
    public void setRimLight(float power, float intensity) {
        if (rimPowerLocation >= 0) {
            GL46C.glUniform1f(rimPowerLocation, power);
        }
        if (rimIntensityLocation >= 0) {
            GL46C.glUniform1f(rimIntensityLocation, intensity);
        }
    }
    
    public void setShadowColor(float r, float g, float b) {
        if (shadowColorLocation >= 0) {
            GL46C.glUniform3f(shadowColorLocation, r, g, b);
        }
    }
    
    public void setSpecular(float power, float intensity) {
        if (specularPowerLocation >= 0) {
            GL46C.glUniform1f(specularPowerLocation, power);
        }
        if (specularIntensityLocation >= 0) {
            GL46C.glUniform1f(specularIntensityLocation, intensity);
        }
    }
    
    // ==================== 描边 Uniform 设置方法 ====================
    
    public void setOutlineProjectionMatrix(FloatBuffer matrix) {
        if (outlineProjMatLocation >= 0) {
            matrix.position(0);
            GL46C.glUniformMatrix4fv(outlineProjMatLocation, false, matrix);
        }
    }
    
    public void setOutlineModelViewMatrix(FloatBuffer matrix) {
        if (outlineModelViewMatLocation >= 0) {
            matrix.position(0);
            GL46C.glUniformMatrix4fv(outlineModelViewMatLocation, false, matrix);
        }
    }
    
    public void setOutlineWidth(float width) {
        if (outlineWidthLocation >= 0) {
            GL46C.glUniform1f(outlineWidthLocation, width);
        }
    }
    
    public void setOutlineColor(float r, float g, float b) {
        if (outlineColorLocation >= 0) {
            GL46C.glUniform3f(outlineColorLocation, r, g, b);
        }
    }
    
    // ==================== Getters ====================
    
    public int getMainProgram() { return mainProgram; }
    public int getOutlineProgram() { return outlineProgram; }
    
    public int getPositionLocation() { return positionLocation; }
    public int getNormalLocation() { return normalLocation; }
    public int getUv0Location() { return uv0Location; }
    
    public int getOutlinePositionLocation() { return outlinePositionLocation; }
    public int getOutlineNormalLocation() { return outlineNormalLocation; }
    
    public boolean isInitialized() { return initialized; }
    
    // ==================== 资源释放 ====================
    
    public void cleanup() {
        if (mainProgram > 0) {
            GL46C.glDeleteProgram(mainProgram);
            mainProgram = 0;
        }
        if (outlineProgram > 0) {
            GL46C.glDeleteProgram(outlineProgram);
            outlineProgram = 0;
        }
        initialized = false;
    }
}
