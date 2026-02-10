# 致谢

本项目的实现离不开以下开源项目及其贡献者的工作。

---

## 核心依赖

### 物理引擎

#### Rapier 物理引擎
- **仓库**: https://github.com/dimforge/rapier
- **作者**: Sébastien Crozet (Dimforge)
- **许可证**: Apache-2.0
- **说明**: 用于游戏、动画和机器人的 2D/3D 物理引擎
- **用途**: 作为物理模拟后端，替代原 C++ 实现中的 Bullet3

### 数学库

#### glam
- **仓库**: https://github.com/bitshifter/glam-rs
- **作者**: Cameron Hart
- **许可证**: MIT 或 Apache-2.0
- **说明**: 简洁高效的 3D 数学库
- **用途**: 主要数学库，用于向量、矩阵和四元数计算

#### nalgebra
- **仓库**: https://github.com/dimforge/nalgebra
- **作者**: Sébastien Crozet (Dimforge)
- **许可证**: Apache-2.0
- **说明**: Rust 线性代数库
- **用途**: Rapier 物理计算依赖

### MMD 格式解析

#### mmd-rs
- **仓库**: https://github.com/aankor/mmd-rs
- **作者**: aankor
- **许可证**: BSD-2-Clause
- **说明**: Rust 实现的 MikuMikuDance 格式解析器
- **用途**: PMX/PMD 模型文件解析

### JNI 绑定

#### jni-rs
- **仓库**: https://github.com/jni-rs/jni-rs
- **作者**: Josh Groves 及贡献者
- **许可证**: MIT 或 Apache-2.0
- **说明**: Rust 的 JNI 绑定库
- **用途**: Rust-Java 交互，用于 Minecraft Mod 集成

---

## 设计参考

### KAIMyEntity（原始 Minecraft MMD 模组）
- **仓库**: https://github.com/kjkjkAIStudio/KAIMyEntity
- **作者**: kjkjkAIStudio, tarsin
- **许可证**: MIT
- **说明**: 在 Minecraft 中加载和显示 MMD 模型的原始模组及配套原生库 (KAIMyEntitySaba)
- **参考内容**:
  - Minecraft MMD 集成方案与渲染逻辑
  - JNI 接口定义与 native 方法签名
  - C++ 原生库架构设计

### KAIMyEntity-C（本项目直接前身）
- **仓库**: https://github.com/Gengorou-C/KAIMyEntity-C
- **作者**: Gengorou-C（基于 kjkjkAIStudio 原作修改）
- **许可证**: MIT
- **说明**: KAIMyEntity 的社区改进版本
- **参考内容**:
  - 本项目的整体 Java 层架构基础


### Bullet3（物理引擎）
- **仓库**: https://github.com/bulletphysics/bullet3
- **作者**: Erwin Coumans 及贡献者
- **许可证**: zlib
- **说明**: 实时碰撞检测和物理模拟库
- **参考内容**:
  - `btGeneric6DofSpringConstraint` 的 6DOF 弹簧约束算法
  - 线性/角度限制和弹簧电机的实现逻辑
  - 约束求解器参数（ERP、CFM）的设计思路

### babylon-mmd（MMD for Babylon.js）
- **仓库**: https://github.com/noname0310/babylon-mmd
- **作者**: noname0310
- **许可证**: MIT
- **说明**: 基于 Babylon.js 的 MMD 运行时实现
- **参考内容**:
  - MMD 物理管线架构（syncBodies/syncBones 流程）
  - 6DOF 弹簧约束的参数设置方式（角度弹簧始终启用、线性弹簧仅 stiffness≠0 启用）
  - PhysicsMode 调整逻辑（父子刚体关系处理）

### Saba（C++ MMD 库）
- **仓库**: https://github.com/benikabocha/saba
- **作者**: benikabocha
- **许可证**: MIT
- **说明**: MMD（PMD/PMX/VMD）播放和加载库，附带查看器
- **参考内容**:
  - 物理系统架构（`MMDPhysics`、`MMDRigidBody`、`MMDJoint`）
  - 坐标系变换（InvZ：左手系转右手系）
  - 刚体偏移矩阵计算
  - 欧拉角转四元数顺序（Y-X-Z）

### nphysics（Rust 物理引擎，已归档）
- **仓库**: https://github.com/dimforge/nphysics
- **作者**: Sébastien Crozet (Dimforge)
- **许可证**: Apache-2.0
- **说明**: 2D/3D 实时物理引擎（已被 Rapier 取代）
- **参考内容**:
  - 骨骼层次管理设计（`Multibody` / `MultibodyLink` 模式）
  - 变换传播算法
  - 排序索引确保父骨骼先于子骨骼更新

### mdanceio（Rust MMD WASM 实现）
- **仓库**: https://github.com/ReaNAiveD/mdanceio
- **作者**: NAiveD
- **许可证**: MIT
- **说明**: 面向浏览器的 MMD 兼容实现（WASM）
- **参考内容**:
  - 动画系统架构
  - 贝塞尔曲线插值

---

## 许可证声明

### Bullet3 (zlib License)

```
Bullet Continuous Collision Detection and Physics Library
http://bulletphysics.org

This software is provided 'as-is', without any express or implied warranty.
In no event will the authors be held liable for any damages arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it freely,
subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
```

### babylon-mmd (MIT License)

```
MIT License

Copyright (c) 2024 noname

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 工具库

| 库名 | 作者 | 许可证 | 用途 |
|------|------|--------|------|
| rayon | Niko Matsakis, Josh Stone | MIT 或 Apache-2.0 | CPU 蒙皮并行计算 |
| thiserror | David Tolnay | MIT 或 Apache-2.0 | 错误类型派生宏 |
| bitflags | The Rust Project Developers | MIT 或 Apache-2.0 | 位标志宏 |
| byteorder | Andrew Gallant | Unlicense 或 MIT | 字节序读写 |
| encoding_rs | Henri Sivonen | MIT 或 Apache-2.0 | 字符编码转换 |
| log | The Rust Project Developers | MIT 或 Apache-2.0 | 日志门面 |
| once_cell | Aleksey Kladov | MIT 或 Apache-2.0 | 惰性静态初始化 |
### 图像处理

#### image (Rust)
- **仓库**: https://github.com/image-rs/image
- **许可证**: MIT 或 Apache-2.0
- **说明**: 用于加载和处理模型纹理（PNG/JPEG/BMP/TGA）
- **参考内容**: 纹理加载流程参考了 C++ `stb_image` 的垂直翻转处理逻辑，以确保与 MMD 渲染规范一致。

---

## 格式规范

### MikuMikuDance 格式

- **使用的格式**:
  - PMX 2.0/2.1（扩展多边形模型）
  - VMD（Vocaloid 动作数据）
  - VPD（Vocaloid 姿势数据）