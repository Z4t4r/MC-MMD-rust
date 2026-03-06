package com.shiroha.mmdskin.renderer.camera;

import com.shiroha.mmdskin.NativeFunc;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * MMD 相机数据
 * 从 Rust 端通过 JNI 获取 CameraFrameTransform，提供位置/旋转/FOV 访问
 */
public class MMDCameraData {
    
    // 32 字节缓冲区: pos(3f) + rot(3f) + fov(1f) + perspective(1i)
    private final ByteBuffer buffer;
    
    // 缓存的变换数据
    private final Vector3f position = new Vector3f();
    private final Vector3f rotation = new Vector3f();
    private float fov = 30.0f;
    private boolean perspective = true;
    
    // 动画句柄
    private long animHandle;
    
    public MMDCameraData() {
        this.buffer = ByteBuffer.allocateDirect(32).order(ByteOrder.nativeOrder());
    }
    
    /**
     * 设置动画句柄
     */
    public void setAnimHandle(long animHandle) {
        this.animHandle = animHandle;
    }
    
    /**
     * 更新相机数据（每帧调用）
     * @param frame 当前浮点帧数
     */
    public void update(float frame) {
        if (animHandle == 0) return;
        
        NativeFunc.GetInst().GetCameraTransform(animHandle, frame, buffer);
        
        buffer.rewind();
        float px = buffer.getFloat();
        float py = buffer.getFloat();
        float pz = buffer.getFloat();
        float rx = buffer.getFloat();
        float ry = buffer.getFloat();
        float rz = buffer.getFloat();
        fov = buffer.getFloat();
        int isPerspective = buffer.getInt();
        
        position.set(px, py, pz);
        rotation.set(rx, ry, rz);
        perspective = isPerspective != 0;
    }
    
    /**
     * 获取 MMD 空间位置
     */
    public Vector3f getPosition() {
        return position;
    }
    
    /**
     * 获取旋转（欧拉角弧度: pitch, yaw, roll）
     */
    public Vector3f getRotation() {
        return rotation;
    }
    
    /**
     * 获取 FOV（度）
     */
    public float getFov() {
        return fov;
    }
    
    /**
     * 是否透视投影
     */
    public boolean isPerspective() {
        return perspective;
    }
    
    /**
     * 获取旋转角 pitch（弧度）
     */
    public float getPitch() {
        return rotation.x;
    }
    
    /**
     * 获取旋转角 yaw（弧度）
     */
    public float getYaw() {
        return rotation.y;
    }
    
    /**
     * 获取旋转角 roll（弧度）
     */
    public float getRoll() {
        return rotation.z;
    }
}
