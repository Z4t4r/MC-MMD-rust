package com.shiroha.mmdskin.renderer.resource;

import com.shiroha.mmdskin.NativeFunc;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;

/**
 * MMD 纹理管理器
 * 负责纹理的加载和缓存，支持两阶段异步加载：
 * 1. preloadTexture() — 后台线程调用，Rust 解码图片 + 拷贝像素到 Java ByteBuffer
 * 2. GetTexture() — 渲染线程调用，如果有预解码数据则只做 GL 上传
 */
public class MMDTextureManager {
    public static final Logger logger = LogManager.getLogger();
    static NativeFunc nf;
    static volatile Map<String, Texture> textures;
    
    /** 后台线程预解码的纹理数据（尚未上传到 GL） */
    private static final Map<String, PredecodedTexture> predecodedTextures = new ConcurrentHashMap<>();

    public static void Init() {
        nf = NativeFunc.GetInst();
        textures = new ConcurrentHashMap<>();
        logger.info("MMDTextureManager 初始化完成");
    }
    
    /**
     * 后台线程预解码纹理（不涉及 GL 调用，可在任意线程调用）
     * 将图片文件通过 Rust 解码为像素数据，存入 Java ByteBuffer 待后续 GL 上传。
     * 
     * @param filename 纹理文件完整路径
     */
    public static void preloadTexture(String filename) {
        // 快速检查：已有 GL 纹理或已预解码，跳过
        Map<String, Texture> localTextures = textures;
        if (localTextures == null) return;
        if (localTextures.containsKey(filename) || predecodedTextures.containsKey(filename)) {
            return;
        }
        
        NativeFunc localNf = NativeFunc.GetInst();
        long nfTex = localNf.LoadTexture(filename);
        if (nfTex == 0) {
            return;
        }
        
        try {
            int x = localNf.GetTextureX(nfTex);
            int y = localNf.GetTextureY(nfTex);
            long texData = localNf.GetTextureData(nfTex);
            boolean hasAlpha = localNf.TextureHasAlpha(nfTex);
            
            int texSize = x * y * (hasAlpha ? 4 : 3);
            ByteBuffer pixelBuffer = MemoryUtil.memAlloc(texSize);
            localNf.CopyDataToByteBuffer(pixelBuffer, texData, texSize);
            pixelBuffer.rewind();
            
            PredecodedTexture predecoded = new PredecodedTexture();
            predecoded.pixelData = pixelBuffer;
            predecoded.width = x;
            predecoded.height = y;
            predecoded.hasAlpha = hasAlpha;
            
            // 原子放入：并发时只有一个线程成功，失败方释放自己的 buffer 防止泄漏
            PredecodedTexture existing = predecodedTextures.putIfAbsent(filename, predecoded);
            if (existing != null) {
                MemoryUtil.memFree(pixelBuffer);
            }
        } finally {
            localNf.DeleteTexture(nfTex);
        }
    }
    
    /**
     * 清除所有预解码数据（在模型重载时调用）
     */
    public static void clearPreloaded() {
        // 释放所有未消费的预解码缓冲区，防止 native 内存泄漏
        for (PredecodedTexture p : predecodedTextures.values()) {
            if (p.pixelData != null) {
                MemoryUtil.memFree(p.pixelData);
                p.pixelData = null;
            }
        }
        predecodedTextures.clear();
    }

    public static Texture GetTexture(String filename) {
        Texture result = textures.get(filename);
        if (result == null) {
            // 检查是否有后台预解码的数据
            PredecodedTexture predecoded = predecodedTextures.remove(filename);
            if (predecoded != null) {
                // 有预解码数据，只做 GL 上传（极快）
                result = uploadPredecodedTexture(predecoded);
                textures.put(filename, result);
                return result;
            }
            
            // 无预解码数据，走原来的全量加载（同步）
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
            ByteBuffer texBuffer = MemoryUtil.memAlloc(texSize);
            try {
                nf.CopyDataToByteBuffer(texBuffer, texData, texSize);
                texBuffer.rewind();
                if (hasAlpha) {
                    GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, 4);
                    GL46C.glTexImage2D(GL46C.GL_TEXTURE_2D, 0, GL46C.GL_RGBA, x, y, 0, GL46C.GL_RGBA, GL46C.GL_UNSIGNED_BYTE, texBuffer);
                } else {
                    GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, 1);
                    GL46C.glTexImage2D(GL46C.GL_TEXTURE_2D, 0, GL46C.GL_RGB, x, y, 0, GL46C.GL_RGB, GL46C.GL_UNSIGNED_BYTE, texBuffer);
                }
            } finally {
                MemoryUtil.memFree(texBuffer);
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
     * 将预解码的纹理数据上传到 GL（必须在渲染线程调用）
     */
    private static Texture uploadPredecodedTexture(PredecodedTexture predecoded) {
        int tex = GL46C.glGenTextures();
        GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, tex);
        
        if (predecoded.hasAlpha) {
            GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, 4);
            GL46C.glTexImage2D(GL46C.GL_TEXTURE_2D, 0, GL46C.GL_RGBA,
                predecoded.width, predecoded.height, 0,
                GL46C.GL_RGBA, GL46C.GL_UNSIGNED_BYTE, predecoded.pixelData);
        } else {
            GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, 1);
            GL46C.glTexImage2D(GL46C.GL_TEXTURE_2D, 0, GL46C.GL_RGB,
                predecoded.width, predecoded.height, 0,
                GL46C.GL_RGB, GL46C.GL_UNSIGNED_BYTE, predecoded.pixelData);
        }
        
        GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAX_LEVEL, 0);
        GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MIN_FILTER, GL46C.GL_LINEAR);
        GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAG_FILTER, GL46C.GL_LINEAR);
        GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, 0);
        
        // GL 上传完成，释放 off-heap 像素缓冲区
        if (predecoded.pixelData != null) {
            MemoryUtil.memFree(predecoded.pixelData);
            predecoded.pixelData = null;
        }
        
        Texture result = new Texture();
        result.tex = tex;
        result.hasAlpha = predecoded.hasAlpha;
        return result;
    }

    /**
     * 定期清理（由 MMDModelManager.tick() 驱动）
     */
    public static void tick() {
        // 当前版本无延迟释放队列，预留接口
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
    
    /** 后台线程预解码的纹理数据（像素数据 + 尺寸，尚未上传到 GL） */
    static class PredecodedTexture {
        ByteBuffer pixelData;
        int width;
        int height;
        boolean hasAlpha;
    }
}
