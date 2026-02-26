package com.shiroha.mmdskin.renderer.render;

import com.shiroha.mmdskin.NativeFunc;
import com.shiroha.mmdskin.config.PathConstants;
import com.shiroha.mmdskin.renderer.model.MMDModelManager;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 舞台动画远程同步 (SRP - 单一职责原则)
 * 
 * 负责远程玩家舞台动画的加载、应用、结束和断线清理。
 * 从 MmdSkinRendererPlayerHelper 中拆分而来。
 */
public final class StageAnimSyncHelper {
    
    private static final Logger logger = LogManager.getLogger();
    
    // 远程玩家舞台动画句柄追踪（线程安全：外层 ConcurrentHashMap + 内层 CopyOnWriteArrayList）
    private static final Map<UUID, List<Long>> remoteStageAnims = new ConcurrentHashMap<>();
    
    private StageAnimSyncHelper() {
    }
    
    /**
     * 远程玩家舞台动画开始
     * 从本地 StageAnim 目录加载相同的 VMD 文件，合并后应用到远程玩家模型
     * 
     * @param player    远程玩家
     * @param stageData 格式: "packName|file1.vmd|file2.vmd|..."
     */
    public static void startStageAnim(Player player, String stageData) {
        if (stageData == null || stageData.isEmpty()) return;
        
        String[] parts = stageData.split("\\|");
        if (parts.length < 2) {
            logger.warn("[舞台同步] 无效的舞台数据: {}", stageData);
            return;
        }
        
        String packName = parts[0];
        
        // 校验包名和文件名安全性（防止路径遍历）
        if (!validatePathSafety(packName)) {
            logger.warn("[舞台同步] 不安全的包名: {}", packName);
            return;
        }
        for (int i = 1; i < parts.length; i++) {
            if (!validatePathSafety(parts[i])) {
                logger.warn("[舞台同步] 不安全的文件名: {}", parts[i]);
                return;
            }
        }
        
        // 解析玩家模型
        PlayerModelResolver.Result resolved = PlayerModelResolver.resolve(player);
        if (resolved == null) {
            logger.warn("[舞台同步] 远程玩家 {} 没有 MMD 模型", player.getName().getString());
            return;
        }
        
        // 先清理该玩家之前的舞台动画（如果有）
        cleanupRemoteStageAnim(player.getUUID());
        
        // 从本地 StageAnim 目录查找对应的文件
        File stageDir = new File(PathConstants.getStageAnimDir(), packName);
        if (!stageDir.exists() || !stageDir.isDirectory()) {
            logger.warn("[舞台同步] 本地没有舞台包: {}", packName);
            return;
        }
        
        // 加载并合并 VMD 动画
        long mergedAnim = loadAndMergeAnimations(stageDir, parts);
        if (mergedAnim == 0) return;
        
        // 应用到远程玩家模型
        MMDModelManager.Model mwed = resolved.model();
        NativeFunc nf = NativeFunc.GetInst();
        long modelHandle = mwed.model.getModelHandle();
        nf.TransitionLayerTo(modelHandle, 0, mergedAnim, 0.3f);
        mwed.model.setLayerLoop(1, true);
        mwed.model.changeAnim(0, 1);
        mwed.model.changeAnim(0, 2);
        mwed.entityData.playCustomAnim = true;
        mwed.entityData.playStageAnim = true;
        
        // 记录句柄用于后续清理
        List<Long> tracked = new CopyOnWriteArrayList<>();
        tracked.add(mergedAnim);
        remoteStageAnims.put(player.getUUID(), tracked);
        
        logger.info("[舞台同步] 远程玩家 {} 舞台动画已应用: {} ({} 个VMD)", 
                    resolved.playerName(), packName, parts.length - 1);
    }
    
    /**
     * 远程玩家舞台动画结束
     */
    public static void endStageAnim(Player player) {
        if (player == null) return;
        
        UUID uuid = player.getUUID();
        cleanupRemoteStageAnim(uuid);
        
        // 解析玩家模型并恢复状态
        PlayerModelResolver.Result resolved = PlayerModelResolver.resolve(player);
        if (resolved != null) {
            resolved.model().entityData.playCustomAnim = false;
            resolved.model().entityData.playStageAnim = false;
            // AnimationStateManager 会在下一帧自动恢复 idle/walk 等动画
        }
        
        logger.info("[舞台同步] 远程玩家 {} 舞台动画结束", player.getName().getString());
    }
    
    /**
     * 断线时清理所有远程玩家的舞台动画句柄
     */
    public static void onDisconnect() {
        if (remoteStageAnims.isEmpty()) return;
        NativeFunc nf = NativeFunc.GetInst();
        int count = 0;
        for (Map.Entry<UUID, List<Long>> entry : remoteStageAnims.entrySet()) {
            for (long handle : entry.getValue()) {
                if (handle != 0) {
                    nf.DeleteAnimation(handle);
                    count++;
                }
            }
        }
        remoteStageAnims.clear();
        logger.info("[舞台同步] 断线清理: 释放 {} 个远程舞台动画句柄", count);
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 校验路径安全性（防止路径遍历攻击）
     */
    private static boolean validatePathSafety(String name) {
        return !name.contains("..") && !name.contains("/") && !name.contains("\\");
    }
    
    /**
     * 加载并合并多个 VMD 动画文件
     * 
     * @return 合并后的动画句柄，失败返回 0
     */
    private static long loadAndMergeAnimations(File stageDir, String[] parts) {
        NativeFunc nf = NativeFunc.GetInst();
        List<Long> loadedAnims = new ArrayList<>();
        
        // 加载第一个动作 VMD
        String firstFile = new File(stageDir, parts[1]).getAbsolutePath();
        long mergedAnim = nf.LoadAnimation(0, firstFile);
        if (mergedAnim == 0) {
            logger.warn("[舞台同步] VMD 加载失败: {}", firstFile);
            return 0;
        }
        loadedAnims.add(mergedAnim);
        
        // 合并其余动作 VMD
        for (int i = 2; i < parts.length; i++) {
            String filePath = new File(stageDir, parts[i]).getAbsolutePath();
            long tempAnim = nf.LoadAnimation(0, filePath);
            if (tempAnim != 0) {
                nf.MergeAnimation(mergedAnim, tempAnim);
                loadedAnims.add(tempAnim);
            }
        }
        
        // 释放临时句柄（保留 mergedAnim）
        for (int i = 1; i < loadedAnims.size(); i++) {
            nf.DeleteAnimation(loadedAnims.get(i));
        }
        
        return mergedAnim;
    }
    
    /**
     * 清理远程玩家的舞台动画句柄
     */
    private static void cleanupRemoteStageAnim(UUID playerUUID) {
        List<Long> anims = remoteStageAnims.remove(playerUUID);
        if (anims != null) {
            NativeFunc nf = NativeFunc.GetInst();
            for (long handle : anims) {
                if (handle != 0) {
                    nf.DeleteAnimation(handle);
                }
            }
        }
    }
}
