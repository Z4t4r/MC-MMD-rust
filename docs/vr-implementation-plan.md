# MC-MMD-rust VR 功能实现计划

> 目标：在 Minecraft 1.20.1 中为 MMD 模型添加 VR 支持，通过 Vivecraft + mc-vr-api 实现头部/手部追踪驱动 MMD 骨骼。

---

## 总体架构

```
┌─────────────────────────────────────────────────────────┐
│                    Java 层 (Mod)                         │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │VRManager  │→│VRBoneDriver  │→│ NativeFunc (JNI)   │  │
│  │(mc-vr-api)│  │(数据转换)     │  │SetVrHandTarget()  │  │
│  └──────────┘  └──────────────┘  └───────────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   Rust 层 (Engine)                       │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │HumanoidBone  │→│VrArmIkSolver │→│ MmdModel       │  │
│  │Map (骨骼映射) │  │(手臂IK求解)   │  │.tick_animation │  │
│  └──────────────┘  └──────────────┘  └───────────────┘  │
└─────────────────────────────────────────────────────────┘
```

---

## Phase 1：基础 VR 兼容（~1-2 周）

确保现有 MMD 模型渲染在 Vivecraft VR 环境中正常工作，无新 VR 交互功能。

### 1.1 添加 mc-vr-api 可选依赖

**文件**: `common/build.gradle`, `fabric/build.gradle`, `forge/build.gradle`

- 添加 mc-vr-api 作为 `compileOnly` 依赖（可选，不强制要求安装）
- Fabric: `modCompileOnly`，Forge: `compileOnly`
- 版本选择与 Minecraft 1.20.1 对应的 mc-vr-api 版本

```gradle
// common/build.gradle
dependencies {
    compileOnly "org.vivecraft:mc-vr-api:${vr_api_version}"
}
```

### 1.2 创建 VRManager 工具类

**新建文件**: `common/src/main/java/com/shiroha/mmdskin/vr/VRManager.java`

职责：
- 运行时检测 Vivecraft 和 mc-vr-api 是否存在（反射，避免硬依赖）
- 封装 IVRAPI 实例获取
- 提供简洁的 API：
  ```java
  public static boolean isVRPresent();        // Vivecraft 是否已加载
  public static boolean isPlayerInVR();       // 玩家当前是否在 VR 模式
  public static Vec3 getHMDPosition();        // 头显世界坐标
  public static Quaternionf getHMDRotation(); // 头显旋转
  public static Vec3 getMainHandPosition();   // 主手控制器位置
  public static Quaternionf getMainHandRotation();
  public static Vec3 getOffHandPosition();    // 副手控制器位置
  public static Quaternionf getOffHandRotation();
  ```

关键设计：
- 所有方法在 VR 不可用时返回 `null` 或默认值
- 使用 `Class.forName()` 检测 Vivecraft 类是否存在，避免 ClassNotFoundException
- 缓存 IVRAPI 实例，避免每帧重新获取

### 1.3 GPU 蒙皮双眼渲染优化

**修改文件**: `common/.../renderer/model/MMDModelGpuSkinning.java`

问题：Vivecraft 对左右眼分别调用 `render()`，Compute Shader 不应每眼都 dispatch。

方案：添加帧号脏标记，确保每逻辑帧只 dispatch 一次蒙皮计算：

```java
private long lastSkinningFrame = -1;

@Override
public void render(...) {
    long currentFrame = Minecraft.getInstance().level.getGameTime();
    if (currentFrame != lastSkinningFrame) {
        dispatchComputeShader(); // 蒙皮计算（每帧一次）
        lastSkinningFrame = currentFrame;
    }
    drawModel(); // 渲染（每眼一次）
}
```

### 1.4 VR 头部追踪接入

**修改文件**: `common/.../renderer/core/HeadAngleHelper.java`

在 `updateHeadAngle()` 开头增加 VR 分支：

```java
public static void updateHeadAngle(NativeFunc nf, long modelHandle,
        LivingEntity entity, float entityYaw, float tickDelta, RenderContext context) {
    
    if (VRManager.isPlayerInVR()) {
        Quaternionf hmdRot = VRManager.getHMDRotation();
        if (hmdRot != null) {
            // 四元数 → 欧拉角（pitch, yaw, roll）
            float pitch = extractPitch(hmdRot);
            float yaw = extractYaw(hmdRot) - entityYaw * (Math.PI / 180f);
            nf.SetHeadAngle(modelHandle, pitch, yaw, 0.0f, context.isWorldScene());
            return;
        }
    }
    
    // 原有逻辑（非 VR）
    // ...
}
```

### 1.5 物品栏渲染检测修复

**修改文件**: `common/.../renderer/render/MmdSkinRenderer.java`

问题：当前使用调用栈检测 `calledFrom("Inventory")`（第 95 行），VR 中调用栈可能不同。

方案：改为使用已有的 `RenderContext` 枚举，由调用方显式传入，而非依赖调用栈。

### 1.6 物理参数 VR 适配

**修改文件**: `common/.../config/ConfigData.java`

- VR 模式下自动调整物理 FPS（60 → 120）
- VR 瞬移后自动调用 `ResetPhysics()` 防止物理爆炸
- 可在 VRManager 中监听玩家位置突变：
  ```java
  if (positionDelta > TELEPORT_THRESHOLD) {
      nf.ResetPhysics(modelHandle);
  }
  ```

### Phase 1 验证标准

- [ ] 安装 Vivecraft + 本 Mod，MMD 模型正常立体渲染（双眼无错位）
- [ ] GPU 蒙皮模式下帧率无额外下降（对比非 VR）
- [ ] 模型头部跟随 VR 头显旋转
- [ ] 眼球追踪在 VR 下自然看向玩家头显位置
- [ ] 无 Vivecraft 时 Mod 功能完全不受影响（无硬依赖）

---

## Phase 2：VR 手臂 IK（~2-3 周）

核心特性：VR 控制器位置/旋转驱动 MMD 模型手臂骨骼。

### 2.1 Rust 侧：HumanoidBoneMap — 骨骼自动映射

**新建文件**: `rust_engine/src/skeleton/humanoid_map.rs`

```rust
/// MMD 标准人体骨骼映射
/// 自动从 BoneManager 中按名称匹配 MMD 标准骨骼
pub struct HumanoidBoneMap {
    // 上半身
    pub head: Option<usize>,          // 頭
    pub neck: Option<usize>,          // 首
    pub upper_body: Option<usize>,    // 上半身
    pub lower_body: Option<usize>,    // 下半身
    pub center: Option<usize>,        // センター
    
    // 右臂
    pub right_shoulder: Option<usize>,   // 右肩
    pub right_upper_arm: Option<usize>,  // 右腕
    pub right_lower_arm: Option<usize>,  // 右ひじ
    pub right_hand: Option<usize>,       // 右手首
    
    // 左臂
    pub left_shoulder: Option<usize>,    // 左肩
    pub left_upper_arm: Option<usize>,   // 左腕
    pub left_lower_arm: Option<usize>,   // 左ひじ
    pub left_hand: Option<usize>,        // 左手首
    
    // 右腿
    pub right_upper_leg: Option<usize>,  // 右足
    pub right_lower_leg: Option<usize>,  // 右ひざ
    pub right_foot: Option<usize>,       // 右足首
    pub right_foot_ik: Option<usize>,    // 右足ＩＫ
    
    // 左腿
    pub left_upper_leg: Option<usize>,   // 左足
    pub left_lower_leg: Option<usize>,   // 左ひざ
    pub left_foot: Option<usize>,        // 左足首
    pub left_foot_ik: Option<usize>,     // 左足ＩＫ
    
    // 手指（可选，用于手势映射）
    pub right_thumb: Option<usize>,      // 右親指１
    pub right_index: Option<usize>,      // 右人指１
    pub left_thumb: Option<usize>,       // 左親指１
    pub left_index: Option<usize>,       // 左人指１
}
```

名称匹配策略（每个骨骼位给出候选名称列表）：

```rust
const RIGHT_UPPER_ARM_NAMES: &[&str] = &["右腕", "右うで", "right_arm", "RightArm", "arm_R"];
const RIGHT_LOWER_ARM_NAMES: &[&str] = &["右ひじ", "右ヒジ", "right_elbow", "RightElbow", "elbow_R"];
const RIGHT_HAND_NAMES: &[&str] = &["右手首", "右手", "right_hand", "RightHand", "hand_R"];
// ... 同理其他骨骼
```

- 模型加载后自动调用 `HumanoidBoneMap::auto_detect(&bone_manager)` 一次
- 缓存在 `MmdModel` 结构体中，避免每帧查找
- 如果关键骨骼（上臂/下臂/手首）缺失，标记该侧手臂 IK 不可用

### 2.2 Rust 侧：VrArmIkSolver — 手臂 IK 求解器

**新建文件**: `rust_engine/src/skeleton/vr_ik.rs`

设计方案：基于现有 `IkSolver` 的 CCD-IK 逻辑，专门为 VR 手臂场景优化。

```rust
pub struct VrArmIkSolver {
    /// 是否启用 VR IK
    enabled: bool,
    
    /// IK 目标（VR 控制器位置/旋转，模型局部空间）
    right_hand_target: Option<(Vec3, Quat)>,
    left_hand_target: Option<(Vec3, Quat)>,
    
    /// IK 链参数
    max_iterations: u32,        // 默认 20
    convergence_threshold: f32, // 默认 0.001
    
    /// 手臂骨骼长度缓存（用于约束）
    right_arm_lengths: Option<(f32, f32)>,  // (上臂长, 下臂长)
    left_arm_lengths: Option<(f32, f32)>,
}
```

求解流程（每帧 `tick_animation` 中调用）：

```
1. 将 VR 控制器世界坐标转换为模型局部坐标
   target_local = model_transform.inverse() * controller_world_pos
   
2. 禁用该手臂骨骼的动画评估（防止 VMD 动画覆盖 IK 结果）
   
3. CCD-IK 求解:
   for iteration in 0..max_iterations:
     for bone in [下臂, 上臂, 肩]:  // 从末端到根部
       current_to_target = target_pos - bone.global_pos
       current_to_end = hand.global_pos - bone.global_pos
       rotation = shortest_rotation(current_to_end, current_to_target)
       bone.local_rotation *= rotation
       apply_angle_constraints(bone)  // 肘关节单轴限制
       update_chain_transforms()
     
     if (hand.global_pos - target_pos).length() < threshold:
       break

4. 将 VR 控制器旋转直接应用到手首骨骼
   hand_bone.global_rotation = controller_rotation
```

角度约束（关键，防止不自然的手臂姿势）：

| 骨骼 | 约束 |
|------|------|
| 肩 (右肩) | pitch: [-45°, 90°], yaw: [-90°, 30°], roll: [-30°, 30°] |
| 上臂 (右腕) | pitch: [-180°, 80°], yaw: [-90°, 90°], roll: [-90°, 90°] |
| 下臂 (右ひじ) | **单轴**（弯曲），pitch: [-150°, 0°]，其他轴锁定 |
| 手首 (右手首) | 直接由 VR 控制器旋转驱动，不参与 IK 求解 |

### 2.3 Rust 侧：集成到 MmdModel

**修改文件**: `rust_engine/src/model/runtime.rs`

在 `MmdModel` 结构体中新增：

```rust
pub struct MmdModel {
    // ... 现有字段 ...
    
    // VR 相关
    humanoid_map: Option<HumanoidBoneMap>,
    vr_ik_solver: VrArmIkSolver,
    vr_mode_enabled: bool,
}
```

在 `tick_animation_no_skinning()` 中（动画评估后、物理更新前），插入 VR IK：

```rust
pub fn tick_animation_no_skinning(&mut self, elapsed: f32) {
    self.animation_layer_manager.update(elapsed);
    self.begin_animation();
    
    self.animation_layer_manager.evaluate_normalized(
        &mut self.bone_manager,
        &mut self.morph_manager,
    );
    
    self.apply_vpd_bone_overrides();
    
    // ★ VR IK：在动画评估后覆盖手臂骨骼
    if self.vr_mode_enabled {
        if let Some(ref map) = self.humanoid_map {
            self.vr_ik_solver.solve(&mut self.bone_manager, map);
        }
    }
    
    self.apply_head_rotation();
    self.update_morph_animation();
    // ... 后续不变 ...
}
```

### 2.4 JNI 接口扩展

**修改文件**: `rust_engine/src/jni_bridge/native_func.rs`

新增 JNI 函数：

```rust
/// 启用/禁用 VR 模式
#[no_mangle]
pub extern "system" fn Java_com_shiroha_mmdskin_NativeFunc_SetVrModeEnabled(
    _env: JNIEnv, _class: JClass, model: jlong, enabled: jboolean,
) { ... }

/// 设置 VR 手部 IK 目标
/// hand: 0 = 右手, 1 = 左手
/// pos: 模型局部空间下的目标位置
/// rot: 目标旋转四元数 (w, x, y, z)
#[no_mangle]
pub extern "system" fn Java_com_shiroha_mmdskin_NativeFunc_SetVrHandTarget(
    _env: JNIEnv, _class: JClass, model: jlong,
    hand: jint,
    pos_x: jfloat, pos_y: jfloat, pos_z: jfloat,
    rot_w: jfloat, rot_x: jfloat, rot_y: jfloat, rot_z: jfloat,
) { ... }

/// 清除 VR 手部 IK 目标（恢复动画控制）
#[no_mangle]
pub extern "system" fn Java_com_shiroha_mmdskin_NativeFunc_ClearVrHandTarget(
    _env: JNIEnv, _class: JClass, model: jlong, hand: jint,
) { ... }

/// 获取 HumanoidBoneMap 检测结果（JSON 格式，调试用）
#[no_mangle]
pub extern "system" fn Java_com_shiroha_mmdskin_NativeFunc_GetHumanoidBoneMapInfo(
    mut env: JNIEnv, _class: JClass, model: jlong,
) -> jstring { ... }
```

### 2.5 Java 侧 NativeFunc 声明

**修改文件**: `common/.../NativeFunc.java`

```java
// ========== VR 相关 ==========
public native void SetVrModeEnabled(long model, boolean enabled);
public native void SetVrHandTarget(long model, int hand,
    float posX, float posY, float posZ,
    float rotW, float rotX, float rotY, float rotZ);
public native void ClearVrHandTarget(long model, int hand);
public native String GetHumanoidBoneMapInfo(long model);
```

### 2.6 Java 侧 VR 骨骼驱动

**新建文件**: `common/.../vr/VRBoneDriver.java`

每帧在 `render()` 中调用：

```java
public class VRBoneDriver {
    
    public static void update(NativeFunc nf, long model, Entity entity) {
        if (!VRManager.isPlayerInVR()) return;
        
        // 获取 VR 控制器世界坐标
        Vec3 mainHandPos = VRManager.getMainHandPosition();
        Quaternionf mainHandRot = VRManager.getMainHandRotation();
        
        if (mainHandPos != null && mainHandRot != null) {
            // 世界坐标 → 模型局部坐标
            Vec3 entityPos = entity.position();
            float entityYaw = entity.getYRot();
            
            Vec3 localPos = worldToModelLocal(mainHandPos, entityPos, entityYaw);
            
            nf.SetVrHandTarget(model, 0 /* 右手 */,
                (float) localPos.x, (float) localPos.y, (float) localPos.z,
                mainHandRot.w, mainHandRot.x, mainHandRot.y, mainHandRot.z);
        }
        
        // 副手同理
        Vec3 offHandPos = VRManager.getOffHandPosition();
        Quaternionf offHandRot = VRManager.getOffHandRotation();
        if (offHandPos != null && offHandRot != null) {
            Vec3 localPos = worldToModelLocal(offHandPos, entity.position(), entity.getYRot());
            nf.SetVrHandTarget(model, 1 /* 左手 */,
                (float) localPos.x, (float) localPos.y, (float) localPos.z,
                offHandRot.w, offHandRot.x, offHandRot.y, offHandRot.z);
        }
    }
}
```

### 2.7 渲染流程集成

**修改文件**: `common/.../renderer/model/MMDModelOpenGL.java` (及 GpuSkinning/NativeRender)

在 `render()` 方法中，动画更新前调用 VR 骨骼驱动：

```java
@Override
public void render(Entity entityIn, float entityYaw, ...) {
    // VR 骨骼驱动
    VRBoneDriver.update(nf, model, entityIn);
    
    // 动画更新 + 渲染（现有逻辑不变）
    nf.UpdateModel(model, deltaTime);
    // ...
}
```

### Phase 2 验证标准

- [ ] 模型加载后 `GetHumanoidBoneMapInfo` 正确识别手臂骨骼
- [ ] VR 控制器移动时，MMD 模型手臂自然跟随
- [ ] 肘部弯曲方向正确（不出现反关节）
- [ ] 手臂 IK 与身体动画（idle/walk）平滑共存
- [ ] IK 不影响物理模拟（头发/裙摆仍正常）
- [ ] 非 VR 模式下代码路径完全不执行（零开销）

---

## Phase 3：VR 舞台模式增强（~1 周）

### 3.1 VR 观众模式

**修改文件**: `common/.../renderer/camera/MMDCameraController.java`

VR 下不强制使用 VMD 相机轨迹，让玩家自由移动观看：

```java
if (VRManager.isPlayerInVR()) {
    // VR 模式：不覆盖相机，玩家自由观看 360°
    // 仅播放音频 + 驱动动画
    playAudio();
    updateAnimation();
} else {
    // 非 VR：原有 VMD 相机轨迹
    applyCameraTransform();
}
```

### 3.2 VR 相机跟随模式（可选）

如果玩家选择"跟随相机"，将 VMD 相机位置作为 VR 玩家的传送目标：

- 每帧将 VMD 相机位置设为玩家位置
- 玩家头显旋转 = VMD 相机旋转 + 玩家自然头部旋转偏移
- 注意：频繁改变位置在 VR 中可能引起眩晕，需要提供选项

### 3.3 VR UI 适配

**修改文件**: `common/.../ui/wheel/` 下的轮盘 UI 类

VR 中 2D GUI 需要额外处理：
- Vivecraft 已有将 2D GUI 渲染为世界空间面板的机制
- 轮盘 UI 需要确保在 VR 面板模式下仍可交互
- 可能需要调整轮盘尺寸和交互逻辑

### Phase 3 验证标准

- [ ] VR 中可以 360° 自由观看 MMD 舞蹈表演
- [ ] 音频正常同步播放
- [ ] 轮盘/选择 UI 在 VR 中可正常操作

---

## Phase 4：高级功能（远期规划）

### 4.1 第一人称化身模式

玩家"成为" MMD 模型：
- 利用 `SetMaterialVisible` 隐藏头部附近面片
- 手臂始终渲染在 VR 控制器位置
- 需要精确的面片分组和遮挡逻辑
- **工作量大，建议后期实现**

### 4.2 全身追踪（FBT）

支持额外的 SteamVR Tracker（腰部 + 双脚）：
- 映射到 センター、左足ＩＫ、右足ＩＫ
- 需要完整的全身 IK 重定向系统
- 参考 VRChat IK 2.0 的实现思路
- **复杂度最高，建议最后实现**

### 4.3 手指追踪

支持 Index Controllers / Quest 手势追踪：
- 映射到 MMD 手指骨骼（親指/人指/中指/薬指/小指 × 1-3 节）
- 大部分 MMD 模型都有手指骨骼
- 可选方案：手势预设（握拳/张开/指向/捏合），而非逐指追踪

### 4.4 VR 中实时物理调参

利用 VR 控制器交互，在 3D 空间中拖拽调整物理参数：
- 可视化刚体碰撞体
- 控制器拖拽调整弹簧刚度
- 调试辅助功能

---

## 文件变更清单

### 新建文件

| 文件 | 说明 | Phase |
|------|------|-------|
| `common/.../vr/VRManager.java` | VR 状态检测和数据获取封装 | 1 |
| `common/.../vr/VRBoneDriver.java` | VR 控制器 → 骨骼驱动 | 2 |
| `rust_engine/src/skeleton/humanoid_map.rs` | MMD 骨骼 → 人体骨骼映射 | 2 |
| `rust_engine/src/skeleton/vr_ik.rs` | VR 手臂 IK 求解器 | 2 |

### 修改文件

| 文件 | 修改内容 | Phase |
|------|----------|-------|
| `common/build.gradle` | 添加 mc-vr-api 可选依赖 | 1 |
| `fabric/build.gradle` | 添加 mc-vr-api 可选依赖 | 1 |
| `forge/build.gradle` | 添加 mc-vr-api 可选依赖 | 1 |
| `HeadAngleHelper.java` | VR 头部追踪分支 | 1 |
| `MmdSkinRenderer.java` | 物品栏检测修复 | 1 |
| `MMDModelGpuSkinning.java` | 蒙皮脏标记 | 1 |
| `ConfigData.java` | VR 物理参数自适应 | 1 |
| `NativeFunc.java` | 新增 VR JNI 方法声明 | 2 |
| `native_func.rs` | 新增 VR JNI 函数实现 | 2 |
| `runtime.rs` | 集成 HumanoidBoneMap + VR IK | 2 |
| `lib.rs` | 导出新模块 | 2 |
| `skeleton/mod.rs` | 导出 humanoid_map, vr_ik | 2 |
| `MMDModelOpenGL.java` | 渲染流程集成 VRBoneDriver | 2 |
| `MMDModelGpuSkinning.java` | 渲染流程集成 VRBoneDriver | 2 |
| `MMDModelNativeRender.java` | 渲染流程集成 VRBoneDriver | 2 |
| `MMDCameraController.java` | VR 舞台模式 | 3 |

---

## 性能目标

| 指标 | 目标值 | 备注 |
|------|--------|------|
| VR 帧率 | ≥ 90 FPS | Quest 2/Index 基线 |
| IK 求解耗时 | < 0.2 ms/模型 | 手臂 IK 链短（3-4 骨骼），CCD 20 次迭代 |
| JNI 额外开销 | < 0.05 ms | 每帧 2 次 SetVrHandTarget |
| 内存增加 | < 1 MB | HumanoidBoneMap + VR IK 状态 |
| 非 VR 性能影响 | 0 | `if vr_mode_enabled` 检查后直接跳过 |

---

## 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| mc-vr-api 版本不兼容 | 中 | 高 | 反射加载，编译期不依赖；提供降级路径 |
| MMD 模型骨骼名称不标准 | 低 | 中 | 候选名称列表覆盖主流命名；支持手动映射 |
| IK 解算姿势不自然 | 中 | 中 | 严格角度约束；参考 MMD 原生 IK 参数 |
| VR 中物理抖动 | 中 | 中 | 物理 FPS 提升至 120；瞬移后自动重置 |
| Iris 光影 VR 兼容 | 低 | 低 | Iris 已有 Vivecraft 集成（uniform 支持） |
