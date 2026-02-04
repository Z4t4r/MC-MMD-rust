# Project Context

## Purpose

在 Minecraft 1.20.1 中实现高性能的 MMD（MikuMikuDance）模型渲染和物理模拟。

### 主要目标

- **PMX 模型加载**: 在 Minecraft 中加载和渲染 MMD 模型
- **VMD 动画播放**: 支持骨骼和表情变形的 MMD 动画播放
- **物理模拟**: 使用 Rapier3D 实现头发、衣物、配饰的实时物理效果
- **GPU 蒙皮**: 通过 Compute Shader 实现高性能顶点蒙皮
- **多层动画**: 支持多个动画同时混合播放

## Tech Stack

### Rust 生态（物理引擎和动画系统）
- **Rust 1.70+** - rust_engine 的主要开发语言
- **Rapier3D 0.22** - 3D 物理引擎（头发、衣物物理模拟）
- **glam 0.29** - 3D 数学库（向量、矩阵、四元数）
- **mmd-rs** - PMX/VMD 格式解析器（本地修改版本）
- **JNI 0.21** - Java Native Interface 绑定
- **vek 0.17** - 额外的数学类型支持
- **nalgebra 0.34** - 线性代数库
- **rayon 1.11** - 并行计算

### Java/Gradle（Minecraft Mod 开发）
- **Java 17+** - Minecraft Mod 的主要开发语言
- **Gradle 8.x** - 构建工具
- **Minecraft 1.20.1** - 目标游戏版本
- **Fabric Loader 0.15.11** - Fabric 模组加载器
- **Fabric API 0.92.2** - Fabric 开发 API
- **Forge 47.3.0** - Forge 模组加载器
- **Architectury Plugin** - 跨平台模组开发框架
- **Loom 1.6** - Minecraft 专用 Gradle 插件

### 渲染技术
- **OpenGL** - 底层渲染 API
- **Compute Shader** - GPU 蒙皮计算
- **Iris Shaders** - 光影兼容支持

## Project Conventions

### Code Style

#### Rust 代码风格
- **格式化工具**: 使用 `rustfmt` 进行代码格式化
- **Edition**: Rust 2021 Edition
- **命名约定**:
  - 模块/文件: `snake_case`
  - 结构体/枚举: `PascalCase`
  - 函数/变量: `snake_case`
  - 常量: `SCREAMING_SNAKE_CASE`
- **错误处理**: 使用 `thiserror` 定义错误类型，使用 `Result<T, E>` 和 `?` 运算符
- **异步模式**: 当前项目主要使用同步代码，物理模拟在专用线程中运行

#### Java/Gradle 代码风格
- **格式化工具**: Google Java Style Guide（4 空格缩进）
- **编码**: UTF-8
- **命名约定**:
  - 类: `PascalCase`
  - 方法/变量: `camelCase`
  - 常量: `UPPER_SNAKE_CASE`
  - 包: `lowercase`

### Architecture Patterns

#### 核心架构: JNI 边界分离
- **Rust 层（rust_engine）**:
  - PMX/VMD 格式解析
  - 骨骼层次管理和 IK 求解
  - 物理模拟（Rapier3D）
  - 动画混合和插值
  - 顶点蒙皮计算

- **Java 层（Fabric/Forge）**:
  - OpenGL 模型渲染
  - Compute Shader 管理
  - Minecraft 游戏集成
  - 资源管理和配置
  - 用户界面

#### JNI 边界设计
- Rust 通过 JNI 暴露 C ABI 接口
- Java 通过 JNI 调用 Rust 函数
- 数据传输使用原始数组和直接内存缓冲区
- 避免频繁跨边界调用（性能考虑）

### Testing Strategy

#### Rust 测试
- **单元测试**: 使用 `cargo test` 运行
- **集成测试**: 在 `tests/` 目录下编写
- **性能测试**: 使用 Criterion 进行基准测试
- **测试覆盖**: 核心算法（IK、物理、蒙皮）需要高覆盖率

#### Java 测试
- **单元测试**: JUnit 框架
- **集成测试**: 在 Minecraft 测试环境中运行
- **Mod 测试**: 使用 Fabric/Forge 的测试框架

### Git Workflow

#### 分支策略
- **main**: 主分支，保持稳定可发布状态
- **feature/***: 功能开发分支
- **fix/***: Bug 修复分支
- **refactor/***: 代码重构分支

#### 提交约定
- **提交信息格式**: 使用约定式提交（Conventional Commits）
  - `feat: 添加新功能`
  - `fix: 修复 Bug`
  - `refactor: 代码重构`
  - `docs: 文档更新`
  - `test: 测试相关`
  - `chore: 构建/工具相关`

#### Pull Request 流程
- 所有代码变更通过 PR 合并
- PR 需要关联 OpenSpec 提案（对于重大变更）
- 代码审查通过后方可合并
- CI/CD 检查必须通过

## Domain Context

### MMD（MikuMikuDance）格式

#### PMX 模型格式
- **骨骼（Bones）**: 层次化骨骼结构，支持 IK、旋转/移动继承
- **顶点（Vertices）**: 支持 Biped/Quad 权重蒙皮
- **材质（Materials）**: Toon 着色、环境光、自发光
- **变形（Morphs）**: 顶点、UV、材质变形
- **刚体（Rigid Bodies）**: 物理模拟对象
- **关节（Joints）**: 连接刚体的物理约束

#### VMD 动画格式
- **骨骼关键帧**: 位置、旋转、贝塞尔插值
- **表情关键帧**: 变形权重
- **相机关键帧**: 相机位置和角度

### 物理模拟

#### 刚体动力学
- 使用 Rapier3D 进行物理计算
- 支持静态、动态、运动学刚体
- 碰撞检测和响应

#### IK（逆运动学）
- CC-IK（循环坐标下降）求解器
- 支持多链 IK
- 目标定位和约束处理

### 骨骼动画

#### 蒙皮技术
- **线性混合蒙皮（LBS）**: 标准顶点蒙皮
- **GPU 蒙皮**: 使用 Compute Shader 并行计算
- **双四元数蒙皮**: 减少蒙皮坍塌（可选）

#### 动画混合
- 多层动画混合
- 动画叠加和过渡
- 时间缩放和循环

## Important Constraints

### 技术约束

#### Minecraft 限制
- **版本**: 固定在 Minecraft 1.20.1
- **Mod 加载器**: 需要同时支持 Fabric 和 Forge
- **渲染管道**: 必须与 Minecraft 的渲染系统兼容
- **资源管理**: 遵循 Minecraft 的资源加载机制

#### 性能要求
- **帧率目标**: 60 FPS
- **物理步频**: 60-120 Hz
- **内存使用**: 合理控制（避免内存泄漏）
- **加载时间**: 快速加载大型模型

#### 平台兼容性
- **操作系统**: Windows, Linux, macOS
- **Java 版本**: Java 17+
- **GPU**: 需要支持 Compute Shader（OpenGL 4.3+）

### 设计约束

#### 数据传输优化
- JNI 边界数据传输最小化
- 使用直接内存缓冲区
- 避免频繁的小批量传输

#### 线程安全
- 物理模拟在专用线程
- 渲染在主线程
- 使用适当同步机制

## External Dependencies

### 核心依赖（Rust）

| 库 | 版本 | 许可证 | 用途 |
|----|------|--------|------|
| [Rapier3D](https://rapier.rs) | 0.22 | Apache-2.0 | 3D 物理引擎 |
| [glam](https://github.com/bitshifter/glam-rs) | 0.29 | MIT/Apache-2.0 | 3D 数学库 |
| [mmd-rs](https://github.com/aankor/mmd-rs) | 本地修改 | BSD-2-Clause | PMX/VMD 格式解析 |
| [jni](https://github.com/jni-rs/jni-rs) | 0.21 | MIT/Apache-2.0 | Java 绑定 |
| [thiserror](https://github.com/dtolnay/thiserror) | 2 | MIT/Apache-2.0 | 错误处理 |
| [rayon](https://github.com/rayon-rs/rayon) | 1.11 | MIT/Apache-2.0 | 并行计算 |

### 核心依赖（Java）

| 库 | 版本 | 用途 |
|----|------|------|
| Minecraft | 1.20.1 | 目标游戏 |
| Fabric Loader | 0.15.11 | Fabric 模组加载器 |
| Fabric API | 0.92.2 | Fabric 开发 API |
| Forge | 47.3.0 | Forge 模组加载器 |
| Architectury | 3.4-SNAPSHOT | 跨平台框架 |

### 设计参考项目

| 项目 | 许可证 | 参考内容 |
|------|--------|----------|
| [KAIMyEntity](https://github.com/kjkjkAIStudio/KAIMyEntity) | MIT | 原始 Minecraft MMD 模组 |
| [KAIMyEntity-C](https://github.com/Gengorou-C/KAIMyEntity-C) | MIT | 本项目的直接前身 |
| [Saba](https://github.com/benikabocha/saba) | MIT | 物理系统架构 |
| [nphysics](https://github.com/dimforge/nphysics) | Apache-2.0 | 骨骼层次设计 |
| [mdanceio](https://github.com/ReaNAiveD/mdanceio) | MIT | 动画系统 |

### 构建工具

| 工具 | 版本 | 用途 |
|------|------|------|
| Cargo | - | Rust 构建工具 |
| Gradle | 8.x | Java 构建工具 |
| Architectury Loom | 1.6-SNAPSHOT | Minecraft Gradle 插件 |

## 项目特定约定

### OpenSpec 提案触发条件

#### 需要创建提案的变更
- 新增功能或能力（如新的动画格式支持）
- 破坏性变更（API、数据格式、架构）
- 跨边界的变更（Rust-Java 接口修改）
- 性能优化（改变行为的优化）
- 安全相关变更

#### 无需提案的变更
- Bug 修复（恢复预期行为）
- 拼写错误、格式化、注释
- 依赖更新（非破坏性）
- 测试代码添加
- 文档更新（不涉及规范变更）

### 性能敏感区域
以下区域需要特别注意性能：
- JNI 边界数据传输
- 物理模拟循环
- GPU 蒙皮计算
- IK 求解器
- 动画混合和插值

### 文件组织约定

```
rust_engine/src/
├── animation/    # 动画系统（VMD、混合、插值）
├── model/        # 模型加载和运行时
├── morph/        # 表情变形
├── physics/      # 物理模拟
├── skeleton/     # 骨骼和 IK
├── skinning/     # 顶点蒙皮
├── texture/      # 纹理加载
└── jni_bridge/   # Java 绑定

fabric/ 和 forge/
├── src/main/java/
│   └── com/shiroha233/skinlayers3d/
│       ├── renderer/     # OpenGL 渲染
│       ├── compute/      # Compute Shader
│       ├── model/        # 模型实例管理
│       └── config/       # 配置和设置
```
