package com.shiroha.mmdskin.renderer.core;

import com.shiroha.mmdskin.NativeFunc;

import java.nio.ByteBuffer;

/**
 * 实体动画状态
 * 管理单个实体的动画层状态和手部矩阵
 */
public class EntityAnimState {
    
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
    
    public EntityAnimState(int layerCount) {
        NativeFunc nf = NativeFunc.GetInst();
        this.stateLayers = new State[layerCount];
        this.playCustomAnim = false;
        this.rightHandMat = nf.CreateMat();
        this.leftHandMat = nf.CreateMat();
        this.matBuffer = ByteBuffer.allocateDirect(64);
    }
    
    /**
     * 将所有层标记为脏状态，确保下次状态更新一定触发动画切换
     */
    public void invalidateStateLayers() {
        for (int i = 0; i < stateLayers.length; i++) {
            stateLayers[i] = null;
        }
    }
    
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
    }
    
    public static String getPropertyName(State state) {
        return state.propertyName;
    }
}
