package com.shiroha.mmdskin.compat.vr;

import com.shiroha.mmdskin.ui.network.PlayerModelSyncManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * VR 手臂隐藏判断（SRP：仅负责判断是否应隐藏 Vivecraft 方块手臂）
 * <p>
 * Vivecraft 在 VR 模式下通过 VRArmHelper → renderArmWithItem 渲染方块手臂。
 * 此类提供统一判断逻辑，供 Forge/Fabric Mixin 共用。
 */
public final class VRArmHider {

    private VRArmHider() {}

    /**
     * 当前本地玩家是否处于 VR 模式
     * 通过 VRDetector 守卫隔离 mc-vr-api 类引用
     */
    public static boolean isLocalPlayerInVR() {
        try {
            if (!VRDetector.isAvailable()) return false;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return false;
            return VRBoneDriver.isVRPlayer(player);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 当前本地玩家是否应隐藏 Vivecraft 方块手臂
     * <p>
     * 条件：MMD 模型激活 且 非原版模型
     */
    public static boolean shouldHideVRArms() {
        try {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) return false;

            String playerName = player.getName().getString();
            String selectedModel = PlayerModelSyncManager.getPlayerModel(
                    player.getUUID(), playerName, true);

            if (selectedModel == null || selectedModel.isEmpty()
                    || selectedModel.equals("默认 (原版渲染)")) {
                return false;
            }

            if (selectedModel.equals("VanilaModel")
                    || selectedModel.equalsIgnoreCase("vanila")) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
