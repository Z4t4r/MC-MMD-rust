package com.shiroha.mmdskin.renderer.render;

import com.shiroha.mmdskin.MmdSkinClient;
import com.shiroha.mmdskin.renderer.animation.MMDAnimManager;
import com.shiroha.mmdskin.renderer.core.EntityAnimState;
import com.shiroha.mmdskin.renderer.core.RenderParams;
import com.shiroha.mmdskin.renderer.model.MMDModelManager;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Vector3f;

/**
 * 通用实体动画状态解析器 (OCP - 开闭原则)
 * 
 * 将 MmdSkinRenderer 中的 if-else 动画状态判定逻辑提取到此处，
 * 使渲染器本身只关注渲染流程。
 * 与 AnimationStateManager（专用于玩家）互补，此类用于通用实体。
 */
public final class EntityAnimationResolver {
    
    private EntityAnimationResolver() {
    }
    
    /**
     * 根据实体当前状态解析动画并更新渲染参数
     * 
     * @param entity     目标实体
     * @param model      模型数据
     * @param entityYaw  实体偏航角
     * @param tickDelta  插值因子
     * @param params     输出：渲染参数（bodyYaw/bodyPitch/translation 会被填充）
     */
    public static void resolve(Entity entity, MMDModelManager.Model model,
                                float entityYaw, float tickDelta, RenderParams params) {
        // 计算插值后的身体朝向
        if (entity instanceof LivingEntity living) {
            params.bodyYaw = Mth.rotLerp(tickDelta, living.yBodyRotO, living.yBodyRot);
        } else {
            params.bodyYaw = entityYaw;
        }
        params.bodyPitch = 0.0f;
        params.translation = new Vector3f(0.0f);
        
        // 优先级最高：死亡 / 睡眠
        if (entity instanceof LivingEntity living) {
            if (living.getHealth() <= 0.0f) {
                changeAnimOnce(model, EntityAnimState.State.Die, 0);
                return;
            }
            if (living.isSleeping()) {
                params.bodyYaw = living.getBedOrientation().toYRot() + 180.0f;
                params.bodyPitch = parseFloat(model, "sleepingPitch", 0.0f);
                String transStr = model.properties.getProperty("sleepingTrans");
                if (transStr != null) {
                    params.translation = MmdSkinClient.str2Vec3f(transStr);
                }
                changeAnimOnce(model, EntityAnimState.State.Sleep, 0);
                return;
            }
        }
        
        // 移动状态判定
        boolean hasMovement = entity.getX() - entity.xo != 0.0f 
                           || entity.getZ() - entity.zo != 0.0f;
        
        if (entity.isVehicle() && hasMovement) {
            changeAnimOnce(model, EntityAnimState.State.Driven, 0);
        } else if (entity.isVehicle()) {
            changeAnimOnce(model, EntityAnimState.State.Ridden, 0);
        } else if (entity.isSwimming()) {
            changeAnimOnce(model, EntityAnimState.State.Swim, 0);
        } else if (hasMovement && entity.getVehicle() == null) {
            changeAnimOnce(model, EntityAnimState.State.Walk, 0);
        } else {
            changeAnimOnce(model, EntityAnimState.State.Idle, 0);
        }
    }
    
    /**
     * 状态未变化时不重复切换动画
     */
    private static void changeAnimOnce(MMDModelManager.Model model,
                                        EntityAnimState.State targetState, int layer) {
        if (model.entityData.stateLayers[layer] != targetState) {
            model.entityData.stateLayers[layer] = targetState;
            String property = EntityAnimState.getPropertyName(targetState);
            model.model.changeAnim(MMDAnimManager.GetAnimModel(model.model, property), layer);
        }
    }
    
    /**
     * 安全解析模型属性中的浮点值
     */
    private static float parseFloat(MMDModelManager.Model model, String key, float defaultValue) {
        String value = model.properties.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
