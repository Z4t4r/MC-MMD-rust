package com.shiroha.mmdskin.renderer.core;

import com.shiroha.mmdskin.config.ConfigManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 模型缓存管理器（两级缓存 + 统一 GC）
 * 
 * 与 {@link com.shiroha.mmdskin.renderer.resource.MMDTextureManager} 对齐的生命周期管理：
 * - active：活跃缓存（每帧渲染时通过 get() 更新访问时间）
 * - pendingRelease：待释放缓存（超过空闲 TTL 后自动移入，等待真正释放）
 * 
 * tick() 统一驱动三层淘汰：
 * 1. 空闲检测：active 中超过 IDLE_TTL 未访问的条目 → 移入 pendingRelease
 * 2. TTL 超时：pendingRelease 中超过 PENDING_TTL → 真正 dispose
 * 3. 容量预算：active + pending 总数超过 modelPoolMaxCount → LRU 淘汰 pending
 * 
 * get() 命中 pendingRelease 时直接恢复到 active，避免昂贵的重新加载。
 * 
 * 线程安全：使用 ConcurrentHashMap
 */
public class ModelCache<T> {
    private static final Logger logger = LogManager.getLogger();
    
    /** 活跃缓存 */
    private final Map<String, CacheEntry<T>> active;
    /** 待释放缓存 */
    private final Map<String, CacheEntry<T>> pendingRelease;
    private final String cacheName;
    
    /** 活跃缓存空闲超时（毫秒）：未被访问超过此时间 → 移入 pending */
    private static final long IDLE_TTL_MS = 60_000;
    /** 待释放缓存过期时间（毫秒）：在 pending 中超过此时间 → dispose */
    private static final long PENDING_TTL_MS = 60_000;
    
    public ModelCache(String name) {
        this.cacheName = name;
        this.active = new ConcurrentHashMap<>();
        this.pendingRelease = new ConcurrentHashMap<>();
    }
    
    // ==================== 访问接口 ====================
    
    /**
     * 获取缓存项
     * 优先查 active，其次查 pendingRelease（命中则恢复到 active）。
     */
    public CacheEntry<T> get(String key) {
        // 1. 活跃缓存命中
        CacheEntry<T> entry = active.get(key);
        if (entry != null) {
            entry.updateAccessTime();
            return entry;
        }
        
        // 2. 从待释放队列恢复（免费，避免重新加载）
        entry = pendingRelease.remove(key);
        if (entry != null) {
            entry.updateAccessTime();
            active.put(key, entry);
            return entry;
        }
        
        return null;
    }
    
    /** 添加缓存项到活跃缓存 */
    public void put(String key, T value) {
        active.put(key, new CacheEntry<>(value));
    }
    
    /**
     * 移除缓存项（同时检查 active 和 pendingRelease）
     */
    public CacheEntry<T> remove(String key) {
        CacheEntry<T> entry = active.remove(key);
        if (entry != null) return entry;
        return pendingRelease.remove(key);
    }
    
    /** 活跃缓存大小 */
    public int size() {
        return active.size();
    }
    
    /** 待释放缓存大小 */
    public int pendingSize() {
        return pendingRelease.size();
    }
    
    // ==================== 定期 GC ====================
    
    /**
     * 统一 GC 入口（在渲染线程每帧调用）
     * 三层淘汰：空闲 → pending，pending 超时 → dispose，超容量 → LRU dispose
     */
    public void tick(Consumer<T> disposer) {
        long now = System.currentTimeMillis();
        
        // 1. 空闲检测：将长时间未访问的 active 条目移入 pendingRelease
        List<String> idleKeys = null;
        for (var entry : active.entrySet()) {
            if (now - entry.getValue().lastAccessTime > IDLE_TTL_MS) {
                if (idleKeys == null) idleKeys = new ArrayList<>();
                idleKeys.add(entry.getKey());
            }
        }
        if (idleKeys != null) {
            for (String key : idleKeys) {
                CacheEntry<T> entry = active.remove(key);
                if (entry != null) {
                    entry.pendingSince = now;
                    pendingRelease.put(key, entry);
                }
            }
        }
        
        if (pendingRelease.isEmpty()) return;
        
        // 2. TTL 超时：释放在 pending 中停留超过 PENDING_TTL 的条目
        List<String> expired = null;
        for (var entry : pendingRelease.entrySet()) {
            if (now - entry.getValue().pendingSince > PENDING_TTL_MS) {
                if (expired == null) expired = new ArrayList<>();
                expired.add(entry.getKey());
            }
        }
        if (expired != null) {
            for (String key : expired) {
                CacheEntry<T> entry = pendingRelease.remove(key);
                if (entry != null) {
                    safeDispose(disposer, entry.value, key);
                }
            }
        }
        
        // 3. 容量预算：总数超过上限时按 LRU 淘汰 pending
        int maxSize = ConfigManager.getModelPoolMaxCount();
        int totalSize = active.size() + pendingRelease.size();
        if (totalSize > maxSize && !pendingRelease.isEmpty()) {
            evictPendingByLRU(totalSize - maxSize, disposer);
        }
    }
    
    /**
     * 按 LRU 淘汰 pending 中的条目
     */
    private synchronized void evictPendingByLRU(int evictCount, Consumer<T> disposer) {
        if (pendingRelease.isEmpty() || evictCount <= 0) return;
        
        List<Map.Entry<String, CacheEntry<T>>> sorted = new ArrayList<>(pendingRelease.entrySet());
        sorted.sort((a, b) -> Long.compare(a.getValue().lastAccessTime, b.getValue().lastAccessTime));
        
        int evicted = 0;
        for (var entry : sorted) {
            if (evicted >= evictCount) break;
            CacheEntry<T> removed = pendingRelease.remove(entry.getKey());
            if (removed != null) {
                safeDispose(disposer, removed.value, entry.getKey());
                evicted++;
            }
        }
        if (evicted > 0) {
        }
    }
    
    // ==================== 批量操作 ====================
    
    /**
     * 移除所有匹配的条目并 dispose（同时检查 active 和 pending）
     */
    public void removeMatching(Predicate<String> keyMatcher, Consumer<T> disposer) {
        removeMatchingFrom(active, keyMatcher, disposer);
        removeMatchingFrom(pendingRelease, keyMatcher, disposer);
    }
    
    private void removeMatchingFrom(Map<String, CacheEntry<T>> map,
                                     Predicate<String> keyMatcher,
                                     Consumer<T> disposer) {
        var it = map.entrySet().iterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (keyMatcher.test(entry.getKey())) {
                it.remove();
                safeDispose(disposer, entry.getValue().value, entry.getKey());
            }
        }
    }
    
    /** 清空所有缓存（active + pending） */
    public synchronized void clear(Consumer<T> disposer) {
        int activeCount = active.size();
        for (CacheEntry<T> entry : active.values()) {
            safeDispose(disposer, entry.value, null);
        }
        active.clear();
        
        int pendingCount = pendingRelease.size();
        for (CacheEntry<T> entry : pendingRelease.values()) {
            safeDispose(disposer, entry.value, null);
        }
        pendingRelease.clear();
    }
    
    /** 遍历活跃缓存（供 PerformanceHud 等统计使用） */
    public void forEach(BiConsumer<String, CacheEntry<T>> action) {
        active.forEach(action);
    }
    
    // ==================== 内部工具 ====================
    
    private void safeDispose(Consumer<T> disposer, T value, String key) {
        try {
            if (disposer != null) {
                disposer.accept(value);
            }
        } catch (Exception e) {
            logger.error("[{}] 释放失败: {}", cacheName, key, e);
        }
    }
    
    /**
     * 缓存条目
     */
    public static class CacheEntry<T> {
        public final T value;
        /** 最后访问时间 */
        public volatile long lastAccessTime;
        /** 移入 pendingRelease 的时间 */
        volatile long pendingSince;
        
        public CacheEntry(T value) {
            this.value = value;
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
