# MC-MMD-rust High-Level Learning Guide

本指南提供 MC-MMD-rust 项目的高层概念和学习路径，帮助快速理解项目架构。

## 快速导航

- [核心概念](#核心概念)
- [架构概览](#架构概览)
- [数据流](#数据流)
- [代码阅读路线](#代码阅读路线)
- [常见问题](#常见问题)

---

## 核心概念

### 什么是 MMD？

**MikuMikuDance (MMD)** 是一个日本开发的3D动画软件，广泛用于创作虚拟歌手动画。

- **PMX**: 模型格式（定义3D角色的形状、材质、骨骼）
- **VMD**: 动画格式（定义骨骼运动和表情变化）
- **物理模拟**: 头发、裙摆等部件的物理摆动效果

### 这个项目做什么？

在 Minecraft 游戏中渲染 MMD 模型和动画：

```
Minecraft 玩家 → 替换为 MMD 模型 → 播放 MMD 动画 → 物理模拟
```

---

## 架构概览

### 为什么需要两种语言？

```
┌─────────────────────────────────────────────────────────┐
│                    Minecraft (Java)                      │
│  - 游戏逻辑                                              │
│  - OpenGL 渲染                                           │
│  - 用户交互                                              │
└───────────────────────────┬─────────────────────────────┘
                            │ JNI
                            ↓
┌─────────────────────────────────────────────────────────┐
│                    Rust Engine                           │
│  - 物理计算（高性能）                                     │
│  - 动画处理（数学密集）                                   │
│  - 内存管理（安全高效）                                   │
└─────────────────────────────────────────────────────────┘
```

### 模块职责

| 模块 | 语言 | 职责 |
|------|------|------|
| `rust_engine` | Rust | 物理模拟、动画计算、PMX/VMD解析 |
| `common` | Java | OpenGL渲染、资源加载、UI界面 |
| `fabric` | Java | Fabric平台适配（事件、注册） |
| `neoforge` | Java | NeoForge平台适配 |

---

## 数据流

### 模型加载流程

```
1. 用户操作：在游戏中选择模型
2. Java层：读取 PMX 文件
3. JNI传递：模型数据传给 Rust
4. Rust层：解析骨骼、顶点、物理数据
5. 初始化：创建物理世界、骨骼层次
```

### 渲染循环（每帧）

```
1. Java：调用 Rust 更新物理
2. Rust：计算新的骨骼位置
3. Rust：计算顶点蒙皮
4. JNI：骨骼数据传回 Java
5. Java：OpenGL 渲染模型
6. GPU：Compute Shader 蒙皮
```

### 关键数据结构

```rust
// Rust 侧核心数据
struct PMXModel {
    vertices: Vec<Vertex>,      // 顶点
    bones: Vec<Bone>,           // 骨骼
    materials: Vec<Material>,   // 材质
    physics: Option<Physics>,   // 物理数据
}
```

```java
// Java 侧核心数据
class ModelEntity {
    Long modelPtr;              // 指向 Rust 模型的指针
    String animationPath;       // 当前动画
    Matrix4f poseMatrix;        // 姿态矩阵
}
```

---

## 代码阅读路线

### 入门：理解项目结构

```
推荐阅读顺序：
1. openspec/project.md          - 项目概述
2. README.md                    - 用户文档
3. gradle.properties            - 版本和依赖
```

### 简单：配置系统（纯 Java）

```
路径：common/src/main/java/com/shiroha/mmdskin/config/

- ModConfig.java       - 配置定义
- ConfigManager.java   - 配置管理

为什么从这里开始？
- 不涉及复杂的 JNI 交互
- 理解 Minecraft Mod 的配置模式
- 了解 Cloth Config 库的使用
```

### 中等：模型加载（Java + Rust）

```
Java 侧：
- common/src/main/java/com/shiroha/mmdskin/resource/
  - ModelLoader.java    - 模型加载入口
  - ResourceManager.java - 资源管理

Rust 侧：
- rust_engine/src/jni_bridge/  - JNI 接口
- rust_engine/src/model/       - 模型解析
- rust_engine/src/skeleton/    - 骨骼系统

重点关注：
- Java 如何调用 Rust 函数
- 指针如何在 JVM 和 Native 之间传递
- 内存如何管理（谁负责释放）
```

### 复杂：物理引擎（Rust + Rapier3D）

```
路径：rust_engine/src/physics/

- rigid_body.rs   - 刚体模拟
- constraint.rs   - 约束（关节）
- world.rs        - 物理世界管理

理解难点：
- Rapier3D 物理引擎 API
- MMD 物理格式到 Rapier 的转换
- 物理步进与游戏帧率的同步
```

### 进阶：渲染管线（Java + OpenGL）

```
路径：common/src/main/java/com/shiroha/mmdskin/renderer/

- ModelRenderer.java        - 渲染主流程
- ComputeShaderSkinning.java - GPU 蒙皮
- shader/                   - 着色器代码

关键概念：
- Compute Shader 如何加速蒙皮
- 如何与 Iris 光影兼容
- 材质和纹理管理
```

---

## 关键技术点

### JNI 接口

**问题**：Java 和 Rust 如何通信？

**方案**：JNI (Java Native Interface)

```java
// Java 侧声明
private native long loadModel(String path);
private native void updatePhysics(long modelPtr, float deltaTime);

// Rust 侧实现
#[no_mangle]
pub extern "system" fn Java_com_shiroha_mmdskin_ModelLoader_loadModel(
    env: JNIEnv,
    _class: JClass,
    path: JString,
) -> jlong {
    // 解析路径，加载模型，返回指针
}
```

**内存安全**：
- Rust 持有数据，返回指针给 Java
- Java 存储指针（`long` 类型），不直接访问内存
- 需要释放时调用 Rust 的 `freeModel(ptr)`

### GPU 蒙皮

**传统 CPU 蒙皮**：
```
每个顶点 → 遍历骨骼 → 计算加权位置 → 更新顶点
复杂度：O(顶点数 × 骨骼数)
```

**GPU Compute Shader 蒙皮**：
```
所有顶点 → 并行处理 → GPU 计算优势
复杂度：O(顶点数 / GPU 核心数)
```

**代码位置**：
- 着色器：`common/src/main/resources/assets/mmdskin/shader/`
- Java 调用：`renderer/ComputeShaderSkinning.java`

### 物理模拟

**MMD 物理特点**：
- 刚体（rigid body）：骨骼关联的物理形状
- 约束（joint）：连接刚体的关节
- 弹簧：影响刚体的力

**Rapier3D 集成**：
```rust
// rust_engine/src/physics/world.rs
pub struct PhysicsWorld {
    rapier_world: RapierWorld,
    rigid_bodies: Vec<RigidBody>,
    joints: Vec<Joint>,
}
```

**同步策略**：
- 物理步长固定（如 60Hz）
- 渲染帧率可变
- 使用插值避免抖动

---

## 常见问题

### Q1: 为什么 README 写的是 1.20.1，但实际是 1.21.1？

**A**: 文档滞后。当前项目针对 Minecraft 1.21.1。

- 查看真实版本：`gradle.properties` 的 `minecraft_version`
- 项目可能从 1.20.1 升级到 1.21.1
- README 需要更新（可作为入门贡献）

### Q2: Shader 缓存是什么？为什么需要？

**A**: 优化性能，避免重复编译。

```
问题：
- 每次 OpenGL 启动需编译 Shader
- Shader 编译耗时（尤其复杂着色器）

方案：
- 首次编译后缓存到磁盘
- 后续启动直接加载缓存

代码：
- renderer/shader/ShaderCache.java
```

### Q3: JNI 内存如何管理？

**A**: Rust 所有权 + Java 显式释放。

```
规则：
1. Rust 分配的内存由 Rust 管理
2. Java 只持有指针（long 类型）
3. 必须调用 native 方法释放
4. 使用 try-finally 确保释放

示例：
long ptr = loadModel(path);
try {
    useModel(ptr);
} finally {
    freeModel(ptr);  // 必须！
}
```

### Q4: 如何调试多语言代码？

**A**: 分层调试。

```
Java 层：
- IDEA 断点调试
- 查看日志（log4j 配置）

Rust 层：
- println! 调试（简单）
- gdb/lldb 调试（复杂）
- Rust 日志重定向到 Java

JNI 边界：
- 检查指针是否为 null
- 确认类型转换正确
- 查看崩溃日志（hs_err_pidXXX.log）
```

### Q5: 为什么用 Rust 不用 C++？

**A**: 内存安全 + 生态系统。

```
Rust 优势：
- 编译时内存安全检查
- 无 GC，性能可预测
- Cargo 依赖管理友好
- 与 C 互操作性好

项目考虑：
- 物理计算密集，Rust 性能接近 C++
- JNI 接口相对简单
- 代码维护成本低
```

---

## 学习建议

### 实践步骤

1. **运行项目**：
   ```bash
   ./gradlew runClient
   ```
   观察实际效果，建立直观认识

2. **修改简单功能**：
   - 修改配置默认值
   - 调整 UI 界面
   - 添加日志输出

3. **追踪数据流**：
   - 从模型文件到屏幕的完整流程
   - 使用断点追踪关键函数调用

4. **阅读关键代码**：
   - 按照上述"代码阅读路线"
   - 每个模块理解核心概念即可

5. **实验和调试**：
   - 修改物理参数看效果
   - 更换模型和动画
   - 测试性能（开启/关闭 GPU 蒙皮）

### 资源推荐

**MMD 相关**：
- [MMD Wiki](https://www.vocaloid.com/wiki/) - MMD 格式规范
- [PMX 规范](https://gist.github.com/jpd002/5856925) - PMX 文件格式

**技术栈**：
- [Rust Book](https://doc.rust-lang.org/book/) - Rust 入门
- [JNI 规范](https://docs.oracle.com/javase/8/docs/technotes/guides/jni/) - Java Native Interface
- [Rapier 文档](https://rapier.rs/docs/) - 物理引擎

**Minecraft Mod**：
- [Fabric 官方文档](https://fabricmc.net/wiki/) - Mod 开发
- [Fabric API 文档](https://fabricmc.net/wiki/documentation:fabric_api) - API 参考

---

## 总结

MC-MMD-rust 是一个优秀的多语言项目示例：

- **Rust**：处理计算密集型任务（物理、动画）
- **Java**：处理游戏集成和渲染
- **JNI**：连接两个世界

学习时：
- 从高层概念开始，建立心智模型
- 逐步深入细节，理解实现选择
- 动手实践，巩固理解

有问题随时询问！
