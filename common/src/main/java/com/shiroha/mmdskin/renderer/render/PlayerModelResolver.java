package com.shiroha.mmdskin.renderer.render;

import com.shiroha.mmdskin.config.UIConstants;
import com.shiroha.mmdskin.renderer.model.MMDModelManager;
import com.shiroha.mmdskin.ui.network.PlayerModelSyncManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家模型解析工具 (DIP - 依赖倒置原则)
 * 
 * 将「获取玩家当前 MMD 模型」的重复逻辑集中到一处，
 * 消除 PlayerHelper / StageAnimSyncHelper / MorphSyncHelper 中的模板代码。
 */
public final class PlayerModelResolver {
    
    private PlayerModelResolver() {
    }
    
    /**
     * 解析结果：包含模型名和玩家显示名
     */
    public record Result(MMDModelManager.Model model, String playerName) {}
    
    /**
     * 解析玩家当前绑定的 MMD 模型
     * 
     * @param player 目标玩家
     * @return 解析成功返回 Result，模型不存在或为默认渲染时返回 null
     */
    public static Result resolve(Player player) {
        if (player == null) return null;
        
        String playerName = player.getName().getString();
        Minecraft mc = Minecraft.getInstance();
        boolean isLocalPlayer = mc.player != null && mc.player.getUUID().equals(player.getUUID());
        String selectedModel = PlayerModelSyncManager.getPlayerModel(player.getUUID(), playerName, isLocalPlayer);
        
        if (selectedModel == null || selectedModel.isEmpty() 
                || selectedModel.equals(UIConstants.DEFAULT_MODEL_NAME)) {
            return null;
        }
        
        MMDModelManager.Model m = MMDModelManager.GetModel(selectedModel, playerName);
        if (m == null) {
            return null;
        }
        
        return new Result(m, playerName);
    }
}
