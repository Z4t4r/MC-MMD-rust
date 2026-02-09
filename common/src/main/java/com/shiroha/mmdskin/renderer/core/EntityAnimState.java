package com.shiroha.mmdskin.renderer.core;

import com.shiroha.mmdskin.NativeFunc;

import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * 实体动画状态
 * 负责管理单个实体的动画层状态和手部矩阵
 */
public class EntityAnimState {
    
    /**
     * 实体状态枚举
     */
    public enum State {
        Idle, Walk, Sprint, Air, 
        OnClimbable, OnClimbableUp, OnClimbableDown, 
        Swim, Ride, Ridden, Driven, 
        Sleep, ElytraFly, Die, 
        SwingRight, SwingLeft, ItemRight, ItemLeft, 
        Sneak, OnHorse, Crawl, LieDown
    }
    
    /**
     * 状态到属性名的映射
     */
    public static final HashMap<State, String> STATE_PROPERTY_MAP = new HashMap<>() {{
        put(State.Idle, "idle");
        put(State.Walk, "walk");
        put(State.Sprint, "sprint");
        put(State.Air, "air");
        put(State.OnClimbable, "onClimbable");
        put(State.OnClimbableUp, "onClimbableUp");
        put(State.OnClimbableDown, "onClimbableDown");
        put(State.Swim, "swim");
        put(State.Ride, "ride");
        put(State.Ridden, "ridden");
        put(State.Driven, "driven");
        put(State.Sleep, "sleep");
        put(State.ElytraFly, "elytraFly");
        put(State.Die, "die");
        put(State.SwingRight, "swingRight");
        put(State.SwingLeft, "swingLeft");
        put(State.ItemRight, "itemRight");
        put(State.ItemLeft, "itemLeft");
        put(State.Sneak, "sneak");
        put(State.OnHorse, "onHorse");
        put(State.Crawl, "crawl");
        put(State.LieDown, "lieDown");
    }};
    
    public boolean playCustomAnim;
    public long rightHandMat;
    public long leftHandMat;
    public State[] stateLayers;
    public ByteBuffer matBuffer;
    
    /**
     * 创建新的实体动画状态
     * 
     * @param layerCount 动画层数量
     */
    public EntityAnimState(int layerCount) {
        NativeFunc nf = NativeFunc.GetInst();
        this.stateLayers = new State[layerCount];
        this.playCustomAnim = false;
        this.rightHandMat = nf.CreateMat();
        this.leftHandMat = nf.CreateMat();
        this.matBuffer = ByteBuffer.allocateDirect(64);
    }
    
    /**
     * 释放资源
     */
    public void dispose() {
        NativeFunc nf = NativeFunc.GetInst();
        if (rightHandMat != 0) {
            nf.DeleteMat(rightHandMat);
            rightHandMat = 0;
        }
        if (leftHandMat != 0) {
            nf.DeleteMat(leftHandMat);
            leftHandMat = 0;
        }
        // matBuffer 由 GC 回收（allocateDirect）
    }
    
    /**
     * 获取状态对应的属性名
     */
    public static String getPropertyName(State state) {
        return STATE_PROPERTY_MAP.get(state);
    }
}
