package com.shiroha.mmdskin.renderer.resource;

import com.shiroha.mmdskin.NativeFunc;
import com.shiroha.mmdskin.config.ConfigManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL46C;
import org.lwjgl.system.MemoryUtil;

/**
 * MMD 纹理管理器
 * 负责纹理的加载、缓存和生命周期管理。
 * 
 * 支持两阶段异步加载：
 * 1. preloadTexture() — 后台线程调用，Rust 解码图片 + 拷贝像素到 Java ByteBuffer
 * 2. GetTexture() — 渲染线程调用，如果有预解码数据则只做 GL 上传
 * 
 * 引用计数 + 延迟释放机制：
 * - 模型创建时通过 addRef() 增加纹理引用计数
 * - 模型 dispose 时通过 release() 减少引用计数
 * - 引用归零后纹理进入延迟释放队列（pendingRelease）
 * - tick() 定期扫描：超过 TTL 或超出 VRAM 软预算时真正释放 GL 纹理
 * - 新模型加载时若命中 pendingRelease 则直接复用，避免重复加载
 */
public class MMDTextureManager {
    private static final Logger logger = LogManager.getLogger();
    private static NativeFunc nf;
    
    /** 活跃纹理（refCount > 0） */
    private static volatile Map<String, Texture> textures;
    
    /** 延迟释放队列（refCount == 0，等待 TTL 超时或预算淘汰） */
    private static final Map<String, Texture> pendingRelease = new ConcurrentHashMap<>();
    
    /** 后台线程预解码的纹理数据（尚未上传到 GL） */
    private static final Map<String, PredecodedTexture> predecodedTextures = new ConcurrentHashMap<>();
    
    /** 延迟释放超时时间（毫秒） */
    private static final long TEXTURE_TTL_MS = 60_000;

    public static void Init() {
        nf = NativeFunc.GetInst();
        textures = new ConcurrentHashMap<>();
        pendingRelease.clear();
    }
    
    /**
     * 后台线程预解码纹理（不涉及 GL 调用，可在任意线程调用）
     * 将图片文件通过 Rust 解码为像素数据，存入 Java ByteBuffer 待后续 GL 上传。
     * 
     * @param filename 纹理文件完整路径
     */
    public static void preloadTexture(String filename) {
        // 快速检查：已有 GL 纹理、待释放纹理或已预解码，跳过
        Map<String, Texture> localTextures = textures;
        if (localTextures == null) return;
        if (localTextures.containsKey(filename) || pendingRelease.containsKey(filename)
                || predecodedTextures.containsKey(filename)) {
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

    /**
     * 获取纹理（渲染线程调用）
     * 优先从活跃缓存获取，其次检查延迟释放队列（复用），最后加载新纹理。
     * 返回的纹理 refCount 不变，需由调用者通过 addRef() 管理。
     */
    public static Texture GetTexture(String filename) {
        // 1. 活跃缓存命中
        Texture result = textures.get(filename);
        if (result != null) {
            return result;
        }
        
        // 2. 检查延迟释放队列（复用已加载但 refCount 归零的纹理）
        result = pendingRelease.remove(filename);
        if (result != null) {
            result.refCount.set(0);
            textures.put(filename, result);
            return result;
        }
        
        // 3. 检查预解码数据
        PredecodedTexture predecoded = predecodedTextures.remove(filename);
        if (predecoded != null) {
            result = uploadPredecodedTexture(predecoded);
            textures.put(filename, result);
            return result;
        }
        
        // 4. 全量同步加载
        long nfTex = nf.LoadTexture(filename);
        if (nfTex == 0) {
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
        result.vramSize = (long) x * y * (hasAlpha ? 4 : 3);
        textures.put(filename, result);
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
        result.vramSize = (long) predecoded.width * predecoded.height * (predecoded.hasAlpha ? 4 : 3);
        return result;
    }

    // ==================== 引用计数管理 ====================
    
    /**
     * 增加纹理引用计数（模型创建时调用）
     * @param filename 纹理文件路径
     */
    public static void addRef(String filename) {
        Texture tex = textures.get(filename);
        if (tex != null) {
            tex.refCount.incrementAndGet();
        }
    }
    
    /**
     * 减少纹理引用计数（模型 dispose 时调用）
     * 引用归零后移入延迟释放队列，等待 TTL 超时或预算淘汰后真正释放。
     * 
     * @param filename 纹理文件路径
     */
    public static void release(String filename) {
        if (filename == null || textures == null) return;
        textures.compute(filename, (key, tex) -> {
            if (tex == null) return null;
            int remaining = tex.refCount.decrementAndGet();
            if (remaining <= 0) {
                tex.refCount.set(0);
                tex.lastReleaseTime = System.currentTimeMillis();
                pendingRelease.put(key, tex);
                return null; // 从 textures 中移除
            }
            return tex;
        });
    }
    
    /**
     * 批量释放纹理引用（模型 dispose 时调用）
     * @param filenames 纹理文件路径列表
     */
    public static void releaseAll(List<String> filenames) {
        if (filenames == null) return;
        for (String filename : filenames) {
            release(filename);
        }
    }
    
    // ==================== 定期 GC ====================
    
    /**
     * 定期扫描延迟释放队列，释放超时或超预算的纹理（在渲染线程调用）
     */
    public static void tick() {
        if (pendingRelease.isEmpty()) return;
        
        long now = System.currentTimeMillis();
        long budgetBytes = ConfigManager.getTextureCacheBudgetMB() * 1024L * 1024L;
        
        // 1. 释放超过 TTL 的纹理
        List<String> expired = new ArrayList<>();
        for (var entry : pendingRelease.entrySet()) {
            if (now - entry.getValue().lastReleaseTime > TEXTURE_TTL_MS) {
                expired.add(entry.getKey());
            }
        }
        for (String key : expired) {
            Texture tex = pendingRelease.remove(key);
            if (tex != null) {
                deleteGlTexture(tex);
            }
        }
        
        long pendingVram = getPendingReleaseVram();
        if (pendingVram > budgetBytes && !pendingRelease.isEmpty()) {
            evictByLRU(pendingVram, budgetBytes);
        }
    }
    
    /**
     * 按 LRU 淘汰延迟释放队列中的纹理，直到 VRAM 降到预算以下
     */
    private static synchronized void evictByLRU(long currentVram, long budgetBytes) {
        if (pendingRelease.isEmpty()) return;
        
        // 按 lastReleaseTime 排序，最早释放的优先淘汰
        List<Map.Entry<String, Texture>> sorted = new ArrayList<>(pendingRelease.entrySet());
        sorted.sort((a, b) -> Long.compare(a.getValue().lastReleaseTime, b.getValue().lastReleaseTime));
        
        long remaining = currentVram;
        int evicted = 0;
        for (var entry : sorted) {
            if (remaining <= budgetBytes) break;
            Texture tex = pendingRelease.remove(entry.getKey());
            if (tex != null) {
                remaining -= tex.vramSize;
                deleteGlTexture(tex);
                evicted++;
            }
        }
        if (evicted > 0) {
        }
    }
    
    /** 删除 GL 纹理对象 */
    private static void deleteGlTexture(Texture tex) {
        if (tex != null && tex.tex > 0) {
            GL46C.glDeleteTextures(tex.tex);
            tex.tex = 0;
        }
    }
    
    // ==================== 清理 ====================
    
    /**
     * 清理所有缓存的纹理（包括活跃和待释放）
     */
    public static void Cleanup() {
        if (textures != null) {
            int count = textures.size();
            for (Texture tex : textures.values()) {
                deleteGlTexture(tex);
            }
            textures.clear();
        }
        int pendingCount = pendingRelease.size();
        for (Texture tex : pendingRelease.values()) {
            deleteGlTexture(tex);
        }
        pendingRelease.clear();
    }
    
    /**
     * 删除单个纹理（强制，不走引用计数）
     */
    public static void DeleteTexture(String filename) {
        if (textures != null) {
            Texture tex = textures.remove(filename);
            deleteGlTexture(tex);
        }
        Texture pending = pendingRelease.remove(filename);
        deleteGlTexture(pending);
    }
    
    // ==================== 统计查询 ====================
    
    public static class Texture {
        public int tex;
        public boolean hasAlpha;
        /** 纹理在 GPU 上的显存占用（字节） */
        public long vramSize;
        /** 引用计数 */
        final AtomicInteger refCount = new AtomicInteger(0);
        /** 引用归零时的时间戳 */
        volatile long lastReleaseTime;
    }
    
    /** 获取活跃纹理的总显存占用（字节） */
    public static long getTotalTextureVram() {
        if (textures == null) return 0;
        long total = 0;
        for (Texture tex : textures.values()) {
            total += tex.vramSize;
        }
        return total;
    }
    
    /** 获取活跃纹理数量 */
    public static int getTextureCount() {
        return textures != null ? textures.size() : 0;
    }
    
    /** 获取延迟释放队列中的纹理数量 */
    public static int getPendingReleaseCount() {
        return pendingRelease.size();
    }
    
    /** 获取延迟释放队列的总 VRAM 占用（字节） */
    public static long getPendingReleaseVram() {
        long total = 0;
        for (Texture tex : pendingRelease.values()) {
            total += tex.vramSize;
        }
        return total;
    }
    
    /** 后台线程预解码的纹理数据（像素数据 + 尺寸，尚未上传到 GL） */
    static class PredecodedTexture {
        ByteBuffer pixelData;
        int width;
        int height;
        boolean hasAlpha;
    }
}
