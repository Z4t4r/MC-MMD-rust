package com.shiroha.mmdskin.renderer.core;

import com.shiroha.mmdskin.config.ConfigManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 模型缓存管理器
 * 负责模型的缓存、LRU清理和生命周期管理
 * 
 * 线程安全：使用 ConcurrentHashMap 保证多线程访问安全
 */
public class ModelCache<T> {
    private static final Logger logger = LogManager.getLogger();
    
    private final Map<String, CacheEntry<T>> cache;
    private final String cacheName;
    
    private long lastSwitchTime = 0;
    private boolean pendingCleanup = false;
    private static final long CLEANUP_DELAY = 60000; // 1 分钟
    
    public ModelCache(String name) {
        this.cacheName = name;
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取缓存项，如果不存在则返回 null
     */
    public CacheEntry<T> get(String key) {
        CacheEntry<T> entry = cache.get(key);
        if (entry != null) {
            entry.updateAccessTime();
        }
        return entry;
    }
    
    /**
     * 添加缓存项
     */
    public void put(String key, T value) {
        CacheEntry<T> entry = new CacheEntry<>(value);
        cache.put(key, entry);
        logger.debug("[{}] 添加缓存: {} (当前: {})", cacheName, key, cache.size());
    }
    
    /**
     * 移除缓存项
     */
    public CacheEntry<T> remove(String key) {
        return cache.remove(key);
    }
    
    /**
     * 检查是否包含指定键
     */
    public boolean containsKey(String key) {
        return cache.containsKey(key);
    }
    
    /**
     * 获取缓存大小
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 记录切换事件，触发延迟清理
     */
    public void onSwitch() {
        lastSwitchTime = System.currentTimeMillis();
        pendingCleanup = true;
    }
    
    /**
     * 定期检查，在渲染循环中调用
     * 
     * @param disposer 清理回调，用于释放资源
     */
    public void tick(Consumer<T> disposer) {
        if (!pendingCleanup) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSwitchTime >= CLEANUP_DELAY) {
            logger.info("[{}] 切换后 1 分钟无操作，清理缓存", cacheName);
            cleanupStale(disposer);
            pendingCleanup = false;
        }
    }
    
    /**
     * 检查并清理缓存（当达到最大容量时）
     * 
     * @param disposer 清理回调
     */
    public void checkAndClean(Consumer<T> disposer) {
        int maxSize = ConfigManager.getModelPoolMaxCount();
        if (cache.size() >= maxSize) {
            cleanupLRU(maxSize, disposer);
        }
    }
    
    /**
     * LRU 清理，保留最近访问的 70%
     */
    private synchronized void cleanupLRU(int maxSize, Consumer<T> disposer) {
        if (cache.size() <= maxSize * 0.8) {
            return;
        }
        
        logger.info("[{}] 清理缓存 (当前: {}, 最大: {})", cacheName, cache.size(), maxSize);
        
        cache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue().lastAccessTime, e2.getValue().lastAccessTime))
            .limit(cache.size() - (int)(maxSize * 0.7))
            .forEach(entry -> {
                try {
                    if (disposer != null) {
                        disposer.accept(entry.getValue().value);
                    }
                    cache.remove(entry.getKey());
                } catch (Exception e) {
                    logger.error("[{}] 清理失败: {}", cacheName, entry.getKey(), e);
                }
            });
        
        logger.info("[{}] 清理完成 (剩余: {})", cacheName, cache.size());
    }
    
    /**
     * 清理过期的缓存（超过 CLEANUP_DELAY 未访问）
     */
    private synchronized void cleanupStale(Consumer<T> disposer) {
        if (cache.isEmpty()) return;
        
        long currentTime = System.currentTimeMillis();
        int cleanedCount = 0;
        
        var iterator = cache.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (currentTime - entry.getValue().lastAccessTime > CLEANUP_DELAY) {
                try {
                    if (disposer != null) {
                        disposer.accept(entry.getValue().value);
                    }
                    iterator.remove();
                    cleanedCount++;
                } catch (Exception e) {
                    logger.error("[{}] 清理失败: {}", cacheName, entry.getKey(), e);
                }
            }
        }
        
        if (cleanedCount > 0) {
            logger.info("[{}] 已清理 {} 个过期缓存", cacheName, cleanedCount);
        }
    }
    
    /**
     * 清空所有缓存
     * 
     * @param disposer 清理回调
     */
    public synchronized void clear(Consumer<T> disposer) {
        for (CacheEntry<T> entry : cache.values()) {
            try {
                if (disposer != null) {
                    disposer.accept(entry.value);
                }
            } catch (Exception e) {
                logger.error("[{}] 清理失败", cacheName, e);
            }
        }
        cache.clear();
        logger.info("[{}] 缓存已清空", cacheName);
    }
    
    /**
     * 遍历所有缓存项
     */
    public void forEach(java.util.function.BiConsumer<String, CacheEntry<T>> action) {
        cache.forEach(action);
    }
    
    /**
     * 缓存条目
     */
    public static class CacheEntry<T> {
        public final T value;
        public long lastAccessTime;
        
        public CacheEntry(T value) {
            this.value = value;
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
