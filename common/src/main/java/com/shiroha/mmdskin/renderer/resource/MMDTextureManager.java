package com.shiroha.mmdskin.renderer.resource;

import com.shiroha.mmdskin.NativeFunc;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL46C;

/**
 * MMD 纹理管理器
 * 负责纹理的加载和缓存
 */
public class MMDTextureManager {
    public static final Logger logger = LogManager.getLogger();
    static NativeFunc nf;
    static Map<String, Texture> textures;

    public static void Init() {
        nf = NativeFunc.GetInst();
        textures = new ConcurrentHashMap<>();
        logger.info("MMDTextureManager 初始化完成");
    }

    public static Texture GetTexture(String filename) {
        Texture result = textures.get(filename);
        if (result == null) {
            long nfTex = nf.LoadTexture(filename);
            if (nfTex == 0) {
                logger.info("纹理未找到: {}", filename);
                return null;
            }
            int x = nf.GetTextureX(nfTex);
            int y = nf.GetTextureY(nfTex);
            long texData = nf.GetTextureData(nfTex);
            boolean hasAlpha = nf.TextureHasAlpha(nfTex);

            int tex = GL46C.glGenTextures();
            GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, tex);
            int texSize = x * y * (hasAlpha ? 4 : 3);
            ByteBuffer texBuffer = ByteBuffer.allocateDirect(texSize);
            nf.CopyDataToByteBuffer(texBuffer, texData, texSize);
            texBuffer.rewind();
            if (hasAlpha) {
                GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, 4);
                GL46C.glTexImage2D(GL46C.GL_TEXTURE_2D, 0, GL46C.GL_RGBA, x, y, 0, GL46C.GL_RGBA, GL46C.GL_UNSIGNED_BYTE, texBuffer);
            } else {
                GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, 1);
                GL46C.glTexImage2D(GL46C.GL_TEXTURE_2D, 0, GL46C.GL_RGB, x, y, 0, GL46C.GL_RGB, GL46C.GL_UNSIGNED_BYTE, texBuffer);
            }
            nf.DeleteTexture(nfTex);

            GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAX_LEVEL, 0);
            GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MIN_FILTER, GL46C.GL_LINEAR);
            GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAG_FILTER, GL46C.GL_LINEAR);
            GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, 0);

            result = new Texture();
            result.tex = tex;
            result.hasAlpha = hasAlpha;
            textures.put(filename, result);
        }
        return result;
    }

    /**
     * 清理所有缓存的纹理
     */
    public static void Cleanup() {
        if (textures != null) {
            int count = textures.size();
            for (Texture tex : textures.values()) {
                if (tex.tex > 0) {
                    GL46C.glDeleteTextures(tex.tex);
                }
            }
            textures.clear();
            logger.info("MMDTextureManager 已清理 {} 个纹理", count);
        }
    }
    
    /**
     * 删除单个纹理
     */
    public static void DeleteTexture(String filename) {
        if (textures != null) {
            Texture tex = textures.remove(filename);
            if (tex != null && tex.tex > 0) {
                GL46C.glDeleteTextures(tex.tex);
            }
        }
    }
    
    public static class Texture {
        public int tex;
        public boolean hasAlpha;
    }
}
