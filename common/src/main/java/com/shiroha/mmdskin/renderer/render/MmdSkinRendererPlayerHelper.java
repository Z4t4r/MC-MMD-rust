package com.shiroha.mmdskin.renderer.render;

import com.shiroha.mmdskin.config.UIConstants;
import com.shiroha.mmdskin.renderer.core.IMMDModel;
import com.shiroha.mmdskin.renderer.animation.MMDAnimManager;
import com.shiroha.mmdskin.renderer.model.MMDModelManager;
import com.shiroha.mmdskin.ui.network.PlayerModelSyncManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class MmdSkinRendererPlayerHelper {

    MmdSkinRendererPlayerHelper() {
    }

    public static void ResetPhysics(Player player) {
        // 从同步管理器获取玩家选择的模型（支持联机）
        String playerName = player.getName().getString();
        Minecraft mc = Minecraft.getInstance();
        boolean isLocalPlayer = mc.player != null && mc.player.getUUID().equals(player.getUUID());
        String selectedModel = PlayerModelSyncManager.getPlayerModel(player.getUUID(), playerName, isLocalPlayer);
        
        // 如果是默认渲染，不处理
        if (selectedModel.equals(UIConstants.DEFAULT_MODEL_NAME) || selectedModel.isEmpty()) {
            return;
        }
        
        MMDModelManager.Model m = MMDModelManager.GetModel(selectedModel, playerName);
        if (m != null) {
            IMMDModel model = m.model;
            ((MMDModelManager.ModelWithEntityData) m).entityData.playCustomAnim = false;
            model.ChangeAnim(MMDAnimManager.GetAnimModel(model, "idle"), 0);
            model.ChangeAnim(0, 1);
            model.ChangeAnim(0, 2);
            model.ResetPhysics();
        }
    }

    public static void CustomAnim(Player player, String id) {
        // 从同步管理器获取玩家选择的模型（支持联机）
        String playerName = player.getName().getString();
        Minecraft mc = Minecraft.getInstance();
        boolean isLocalPlayer = mc.player != null && mc.player.getUUID().equals(player.getUUID());
        String selectedModel = PlayerModelSyncManager.getPlayerModel(player.getUUID(), playerName, isLocalPlayer);
        
        // 如果是默认渲染，不处理
        if (selectedModel.equals(UIConstants.DEFAULT_MODEL_NAME) || selectedModel.isEmpty()) {
            return;
        }
        
        MMDModelManager.Model m = MMDModelManager.GetModel(selectedModel, playerName);
        if (m != null) {
            MMDModelManager.ModelWithEntityData mwed = (MMDModelManager.ModelWithEntityData) m;
            IMMDModel model = m.model;
            mwed.entityData.playCustomAnim = true;
            // 直接使用文件名作为动画ID，MMDAnimManager 会自动在 CustomAnim 目录查找
            model.ChangeAnim(MMDAnimManager.GetAnimModel(model, id), 0);
            model.ChangeAnim(0, 1);
            model.ChangeAnim(0, 2);
        }
    }
}
