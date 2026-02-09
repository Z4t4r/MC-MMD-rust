package com.shiroha.mmdskin.renderer.shader;

import com.shiroha.mmdskin.config.PathConstants;
import java.io.File;
import java.io.FileInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL46C;

public class ShaderProvider {
    private static boolean isInited = false;
    private static int program = 0;
    // 延迟初始化着色器路径
    private static String vertexPath;
    private static String fragPath;
    public static final Logger logger = LogManager.getLogger();

    public static void Init() {
        if (!isInited) {
            // 初始化着色器路径
            File shaderDir = PathConstants.getShaderDir();
            vertexPath = new File(shaderDir, "MMDShader.vsh").getAbsolutePath();
            fragPath = new File(shaderDir, "MMDShader.fsh").getAbsolutePath();
            
            try {
                int vertexShader = GL46C.glCreateShader(GL46C.GL_VERTEX_SHADER);
                try (FileInputStream vertexSource = new FileInputStream(vertexPath)) {
                    GL46C.glShaderSource(vertexShader, new String(vertexSource.readAllBytes()));
                }

                int fragShader = GL46C.glCreateShader(GL46C.GL_FRAGMENT_SHADER);
                try (FileInputStream fragSource = new FileInputStream(fragPath)) {
                    GL46C.glShaderSource(fragShader, new String(fragSource.readAllBytes()));
                }

                GL46C.glCompileShader(vertexShader);
                if (GL46C.glGetShaderi(vertexShader, GL46C.GL_COMPILE_STATUS) == GL46C.GL_FALSE) {
                    String log = GL46C.glGetShaderInfoLog(vertexShader, 8192).trim();
                    logger.error("Failed to compile vertex shader: {}", log);
                    GL46C.glDeleteShader(vertexShader);
                    isInited = true;
                    return;
                }

                GL46C.glCompileShader(fragShader);
                if (GL46C.glGetShaderi(fragShader, GL46C.GL_COMPILE_STATUS) == GL46C.GL_FALSE) {
                    String log = GL46C.glGetShaderInfoLog(fragShader, 8192).trim();
                    logger.error("Failed to compile fragment shader: {}", log);
                    GL46C.glDeleteShader(vertexShader);
                    GL46C.glDeleteShader(fragShader);
                    isInited = true;
                    return;
                }
                program = GL46C.glCreateProgram();
                GL46C.glAttachShader(program, vertexShader);
                GL46C.glAttachShader(program, fragShader);
                GL46C.glLinkProgram(program);
                if (GL46C.glGetProgrami(program, GL46C.GL_LINK_STATUS) == GL46C.GL_FALSE) {
                    String log = GL46C.glGetProgramInfoLog(program, 8192);
                    logger.error("Failed to link shader program\n{}", log);
                    GL46C.glDeleteProgram(program);
                    program = 0;
                    isInited = true;
                    return;
                }
                logger.info("MMD Shader Initialize finished");
            } catch (Exception e) {
                logger.error("MMD Shader initialization failed", e);
                program = 0;
            }
            isInited = true;
        }
    }

    public static boolean isReady() {
        return isInited && program > 0;
    }

    public static int getProgram() {
        if (program <= 0)
            throw new RuntimeException("MMD Shader 未初始化或初始化失败");
        return program;
    }
}
