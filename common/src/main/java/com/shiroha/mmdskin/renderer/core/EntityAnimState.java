package com.shiroha.mmdskin.renderer.core;

import com.shiroha.mmdskin.NativeFunc;

import java.nio.ByteBuffer;

/**
 * 实体动画状态
 * 负责管理单个实体的动画层状态和手部矩阵
 */
public class EntityAnimState {
    
    /**
     * 实体状态枚举（OCP: 属性名内嵌，新增状态只需修改此处）
     */
    public enum State {
        Idle("idle"), Walk("walk"), Sprint("sprint"), Air("air"),
        OnClimbable("onClimbable"), OnClimbableUp("onClimbableUp"), OnClimbableDown("onClimbableDown"),
        Swim("swim"), Ride("ride"), Ridden("ridden"), Driven("driven"),
        Sleep("sleep"), ElytraFly("elytraFly"), Die("die"),
        SwingRight("swingRight"), SwingLeft("swingLeft"), ItemRight("itemRight"), ItemLeft("itemLeft"),
        Sneak("sneak"), OnHorse("onHorse"), Crawl("crawl"), LieDown("lieDown");
        
        public final String propertyName;
        
        State(String propertyName) {
            this.propertyName = propertyName;
        }
    }
    
    public boolean playCustomAnim;
    public boolean playStageAnim;
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
        return state.propertyName;
    }
}
