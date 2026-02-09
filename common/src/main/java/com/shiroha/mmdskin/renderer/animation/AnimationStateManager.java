package com.shiroha.mmdskin.renderer.animation;

import com.shiroha.mmdskin.renderer.core.EntityAnimState;
import com.shiroha.mmdskin.renderer.model.MMDModelManager.ModelWithEntityData;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;

/**
 * 动画状态管理器
 * 负责根据玩家状态切换动画
 * 
 * 设计原则：单一职责，只负责动画状态的判断和切换
 */
public class AnimationStateManager {
    
    private static final float TRANSITION_TIME = 0.25f; // 过渡时间（秒）
    
    /**
     * 更新玩家动画状态
     */
    public static void updateAnimationState(AbstractClientPlayer player, ModelWithEntityData model) {
        if (model.entityData.playCustomAnim) {
            if (shouldStopCustomAnimation(player)) {
                model.entityData.playCustomAnim = false;
            }
        }
        
        if (!model.entityData.playCustomAnim) {
            updateLayer0Animation(player, model);
            updateLayer1Animation(player, model);
            updateLayer2Animation(player, model);
        }
    }
    
    private static boolean shouldStopCustomAnimation(AbstractClientPlayer player) {
        return player.getHealth() == 0.0f ||
               player.isFallFlying() ||
               player.isSleeping() ||
               player.isSwimming() ||
               player.onClimbable() ||
               player.isSprinting() ||
               player.isVisuallyCrawling() ||
               player.isPassenger() ||
               hasMovement(player);
    }
    
    private static boolean hasMovement(AbstractClientPlayer player) {
        return player.getX() - player.xo != 0.0f || player.getZ() - player.zo != 0.0f;
    }
    
    private static void updateLayer0Animation(AbstractClientPlayer player, ModelWithEntityData model) {
        if (player.getHealth() == 0.0f) {
            changeAnimationOnce(model, EntityAnimState.State.Die, 0);
        } else if (player.isFallFlying()) {
            changeAnimationOnce(model, EntityAnimState.State.ElytraFly, 0);
        } else if (player.isSleeping()) {
            changeAnimationOnce(model, EntityAnimState.State.Sleep, 0);
        } else if (player.isPassenger()) {
            updateRidingAnimation(player, model);
        } else if (player.isSwimming()) {
            changeAnimationOnce(model, EntityAnimState.State.Swim, 0);
        } else if (player.onClimbable()) {
            updateClimbingAnimation(player, model);
        } else if (player.isSprinting() && !player.isShiftKeyDown()) {
            changeAnimationOnce(model, EntityAnimState.State.Sprint, 0);
        } else if (player.isVisuallyCrawling()) {
            updateCrawlingAnimation(player, model);
        } else if (hasMovement(player)) {
            changeAnimationOnce(model, EntityAnimState.State.Walk, 0);
        } else {
            changeAnimationOnce(model, EntityAnimState.State.Idle, 0);
        }
    }
    
    private static void updateRidingAnimation(AbstractClientPlayer player, ModelWithEntityData model) {
        var vehicle = player.getVehicle();
        if (vehicle != null && vehicle.getType() == EntityType.HORSE && hasMovement(player)) {
            changeAnimationOnce(model, EntityAnimState.State.OnHorse, 0);
        } else {
            changeAnimationOnce(model, EntityAnimState.State.Ride, 0);
        }
    }
    
    private static void updateClimbingAnimation(AbstractClientPlayer player, ModelWithEntityData model) {
        double verticalMovement = player.getY() - player.yo;
        if (verticalMovement > 0) {
            changeAnimationOnce(model, EntityAnimState.State.OnClimbableUp, 0);
        } else if (verticalMovement < 0) {
            changeAnimationOnce(model, EntityAnimState.State.OnClimbableDown, 0);
        } else {
            changeAnimationOnce(model, EntityAnimState.State.OnClimbable, 0);
        }
    }
    
    private static void updateCrawlingAnimation(AbstractClientPlayer player, ModelWithEntityData model) {
        if (hasMovement(player)) {
            changeAnimationOnce(model, EntityAnimState.State.Crawl, 0);
        } else {
            changeAnimationOnce(model, EntityAnimState.State.LieDown, 0);
        }
    }
    
    private static void updateLayer1Animation(AbstractClientPlayer player, ModelWithEntityData model) {
        if ((!player.isUsingItem() && !player.swinging) || player.isSleeping()) {
            if (model.entityData.stateLayers[1] != EntityAnimState.State.Idle) {
                model.entityData.stateLayers[1] = EntityAnimState.State.Idle;
                model.model.TransitionAnim(0, 1, TRANSITION_TIME);
            }
        } else {
            updateHandAnimation(player, model);
        }
    }
    
    private static void updateHandAnimation(AbstractClientPlayer player, ModelWithEntityData model) {
        if (player.getUsedItemHand() == InteractionHand.MAIN_HAND && player.isUsingItem()) {
            String itemId = getItemId(player, InteractionHand.MAIN_HAND);
            applyCustomItemAnimation(model, EntityAnimState.State.ItemRight, itemId, "Right", "using", 1);
        } else if (player.swingingArm == InteractionHand.MAIN_HAND && player.swinging) {
            String itemId = getItemId(player, InteractionHand.MAIN_HAND);
            applyCustomItemAnimation(model, EntityAnimState.State.SwingRight, itemId, "Right", "swinging", 1);
        } else if (player.getUsedItemHand() == InteractionHand.OFF_HAND && player.isUsingItem()) {
            String itemId = getItemId(player, InteractionHand.OFF_HAND);
            applyCustomItemAnimation(model, EntityAnimState.State.ItemLeft, itemId, "Left", "using", 1);
        } else if (player.swingingArm == InteractionHand.OFF_HAND && player.swinging) {
            String itemId = getItemId(player, InteractionHand.OFF_HAND);
            applyCustomItemAnimation(model, EntityAnimState.State.SwingLeft, itemId, "Left", "swinging", 1);
        }
    }
    
    private static void updateLayer2Animation(AbstractClientPlayer player, ModelWithEntityData model) {
        if (player.isShiftKeyDown() && !player.isVisuallyCrawling()) {
            changeAnimationOnce(model, EntityAnimState.State.Sneak, 2);
        } else {
            if (model.entityData.stateLayers[2] != EntityAnimState.State.Idle) {
                model.entityData.stateLayers[2] = EntityAnimState.State.Idle;
                model.model.TransitionAnim(0, 2, TRANSITION_TIME);
            }
        }
    }
    
    private static void changeAnimationOnce(ModelWithEntityData model, EntityAnimState.State targetState, int layer) {
        String property = EntityAnimState.getPropertyName(targetState);
        if (model.entityData.stateLayers[layer] != targetState) {
            model.entityData.stateLayers[layer] = targetState;
            model.model.TransitionAnim(MMDAnimManager.GetAnimModel(model.model, property), layer, TRANSITION_TIME);
        }
    }
    
    private static void applyCustomItemAnimation(ModelWithEntityData model, EntityAnimState.State targetState, 
                                                  String itemName, String activeHand, String handState, int layer) {
        long anim = MMDAnimManager.GetAnimModel(model.model, 
            String.format("itemActive_%s_%s_%s", itemName, activeHand, handState));
        
        if (anim != 0) {
            if (model.entityData.stateLayers[layer] != targetState) {
                model.entityData.stateLayers[layer] = targetState;
                model.model.TransitionAnim(anim, layer, TRANSITION_TIME);
            }
            return;
        }
        
        if (targetState == EntityAnimState.State.ItemRight || targetState == EntityAnimState.State.SwingRight) {
            changeAnimationOnce(model, EntityAnimState.State.SwingRight, layer);
        } else if (targetState == EntityAnimState.State.ItemLeft || targetState == EntityAnimState.State.SwingLeft) {
            changeAnimationOnce(model, EntityAnimState.State.SwingLeft, layer);
        }
    }
    
    private static String getItemId(AbstractClientPlayer player, InteractionHand hand) {
        String descriptionId = player.getItemInHand(hand).getItem().getDescriptionId();
        return descriptionId.substring(descriptionId.indexOf(".") + 1);
    }
}
