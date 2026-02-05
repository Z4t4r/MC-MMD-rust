# MC-MMD-rust

在 Minecraft 1.20.1 中实现 MMD（MikuMikuDance）模型渲染和物理模拟的 Mod。

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## 功能特性

- **PMX 模型加载**: 在 Minecraft 中加载和渲染 MMD 模型
- **VMD 动画播放**: 支持骨骼和表情变形的 MMD 动画播放
- **物理模拟**: 使用 Rapier3D 实现头发、衣物、配饰的实时物理效果
- **GPU 蒙皮**: 通过 Compute Shader 实现高性能顶点蒙皮
- **多层动画**: 支持多个动画同时混合播放

## 架构

本项目由两个主要部分组成：

1. **rust_engine**: 基于 Rust 的 MMD 物理和动画引擎
   - PMX/VMD 格式解析
   - 骨骼层次管理
   - 物理模拟（Rapier3D）
   - JNI 绑定用于 Java 交互

2. **Minecraft Mod**（Fabric/Forge/NeoForge）: 基于 Java 的渲染和集成
   - OpenGL 模型渲染
   - Compute Shader 蒙皮
   - Iris 光影兼容

## 使用教程

### 安装

1. 将模组 `.jar` 文件放入 `.minecraft/mods/` 目录
2. 启动游戏，模组会自动创建 `3d-skin` 资源目录
3. 将自己的模型和动画文件放入对应目录（详见下文）

### 目录结构

模组使用 `.minecraft/3d-skin/` 作为资源根目录，结构如下：

```
.minecraft/
└── 3d-skin/
    ├── EntityPlayer/      # 玩家模型目录
    │   ├── 模型A/         # 每个子文件夹是一个模型，文件夹名就是模型名
    │   │   ├── model.pmx  # 模型文件（支持 .pmx/.pmd）
    │   │   ├── *.png      # 贴图文件
    │   │   ├── dance.vmd  # 模型专属动画（可选）
    │   │   └── smile.vpd  # 模型专属表情（可选）
    │   └── 模型B/
    │       └── ...
    ├── DefaultAnim/       # 系统预设动画（模组自动释放）
    ├── CustomAnim/        # 用户自定义动画
    ├── DefaultMorph/      # 系统预设表情
    ├── CustomMorph/       # 用户自定义表情
    └── Shader/            # 自定义着色器
```

### 文件夹详解

#### `EntityPlayer/` - 玩家模型目录

存放玩家可用的 MMD 模型。**每个模型必须放在独立的子文件夹中**。

| 文件类型 | 扩展名 | 说明 |
|---------|--------|------|
| 模型文件 | `.pmx` / `.pmd` | 必需，PMX 优先于 PMD |
| 贴图文件 | `.png` / `.jpg` / `.bmp` / `.tga` | 模型引用的贴图 |
| 专属动画 | `.vmd` | 可选，仅该模型可用 |
| 专属表情 | `.vpd` | 可选，仅该模型可用 |

**模型识别规则**：
- 扫描每个子文件夹，查找 `.pmx` 或 `.pmd` 文件
- 若文件夹内有多个模型文件，优先选择 `model.pmx` 或 `model.pmd`
- 若无 `model.*`，则按文件名排序选择第一个

**示例**：
```
EntityPlayer/
├── miku/
│   ├── model.pmx          # ✓ 被加载
│   ├── body.png
│   └── face.png
└── shiroha/
    ├── cirno_v2.pmx       # ✓ 被加载（文件夹内唯一 pmx）
    ├── cirno_old.pmd      # ✗ 忽略（pmx 优先）
    └── texture.png
```

#### `DefaultAnim/` - 系统预设动画

模组首次启动时自动从内置资源释放，包含游戏状态对应的基础动画：

| 动画文件名 | 触发条件 |
|-----------|----------|
| `idle.vmd` | 站立静止 |
| `walk.vmd` | 行走 |
| `sprint.vmd` | 疾跑 |
| `sneak.vmd` | 潜行 |
| `swim.vmd` | 游泳 |
| `crawl.vmd` | 匍匐 |
| `sleep.vmd` | 睡觉 |
| `die.vmd` | 死亡 |
| `elytraFly.vmd` | 鞘翅飞行 |
| `onClimbable.vmd` | 攀爬（静止） |
| `onClimbableUp.vmd` | 攀爬（上） |
| `onClimbableDown.vmd` | 攀爬（下） |
| `onHorse.vmd` / `ride.vmd` | 骑乘 |
| `lieDown.vmd` | 躺下 |
| `swingLeft.vmd` | 左手挥动 |
| `swingRight.vmd` | 右手挥动 |
| `itemActive_*.vmd` | 物品使用动画 |

> **注意**：可直接替换这些文件来自定义基础动画，但建议备份原文件。

#### `CustomAnim/` - 用户自定义动画

存放用户添加的 `.vmd` 动画文件，可通过动作轮盘手动触发播放。

**命名建议**：文件名即为显示名称，建议使用有意义的名称如 `跳舞.vmd`、`打招呼.vmd`。

#### `DefaultMorph/` - 系统预设表情

存放系统预设的 `.vpd` 表情文件（如眨眼、微笑等）。

#### `CustomMorph/` - 用户自定义表情

存放用户添加的 `.vpd` 表情文件，可通过表情轮盘手动触发。

### 动画加载优先级

当需要播放动画时，模组按以下顺序查找：

1. **模型专属目录** (`EntityPlayer/模型名/*.vmd`) - 最高优先级
2. **自定义动画目录** (`CustomAnim/*.vmd`)
3. **默认动画目录** (`DefaultAnim/*.vmd`) - 最低优先级

这意味着你可以为特定模型创建专属动画，覆盖通用动画。

**示例**：若 `EntityPlayer/初音未来/idle.vmd` 存在，则该模型的站立动画使用此文件，而非 `DefaultAnim/idle.vmd`。

### 快捷键操作

#### 主配置轮盘（按住 `Alt` 键）

按住 `Alt` 键打开主配置轮盘，移动鼠标选择功能，松开 `Alt` 确认：

| 选项 | 功能 |
|------|------|
| 🎭 **模型切换** | 打开模型选择界面，切换玩家使用的 MMD 模型 |
| 🎬 **动作选择** | 打开动作轮盘，手动播放 `CustomAnim/` 中的动画 |
| 😊 **表情选择** | 打开表情轮盘，应用 `CustomMorph/` 中的表情 |
| 👕 **材质控制** | 控制模型各部位材质的显示/隐藏 |
| ⚙ **模组设置** | 打开模组配置界面 |

#### 女仆配置轮盘（对准女仆按 `B` 键）

若安装了 TouhouLittleMaid 模组，对准女仆实体按 `B` 键可打开女仆专用配置轮盘。

> **提示**：快捷键可在游戏设置 → 控制 → MMD Skin 分类中自定义。

### 模型切换界面

按住 `Alt` → 选择「模型切换」进入：

- 显示 `EntityPlayer/` 下所有可用模型
- 显示模型格式（PMX/PMD）、文件名、文件大小
- 点击模型卡片立即切换
- 选择「默认」恢复原版玩家皮肤渲染
- 点击「刷新」重新扫描模型目录

### 动作/表情轮盘配置

动作轮盘和表情轮盘支持自定义槽位：

1. 打开对应轮盘界面
2. 点击右下角 ⚙ 按钮进入配置界面
3. 从可用列表中添加/移除槽位
4. 拖拽调整顺序

配置保存在 `.minecraft/config/mmdskin/` 目录下。

## 构建

### 前置要求

- Rust 1.70+（用于 rust_engine）
- JDK 17+（用于 Minecraft mod）
- Gradle 8.x

### 构建 rust_engine

```bash
cd rust_engine
cargo build --release
```

### 构建 Minecraft Mod

```bash
# 构建所有平台（Fabric + Forge + NeoForge）
./gradlew build

# 仅构建特定平台
./gradlew :fabric:build
./gradlew :forge:build
./gradlew :neoforge:build

# 运行客户端（用于测试）
./gradlew :fabric:runClient
./gradlew :forge:runClient
./gradlew :neoforge:runClient
```

### 支持的平台

| 平台 | 版本要求 | 文件名 |
|------|---------|--------|
| Fabric | Fabric 0.15.11+ / Minecraft 1.20.1 | `mmdskin-fabric-{version}.jar` |
| Forge | Forge 47.3.0+ / Minecraft 1.20.1 | `mmdskin-forge-{version}.jar` |
| NeoForge | NeoForge 47.1.81+ / Minecraft 1.20.1 | `mmdskin-neoforge-{version}.jar` |

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。

## 致谢

本项目基于众多开源项目和贡献者的工作。
完整致谢请参阅 [ACKNOWLEDGMENTS.md](ACKNOWLEDGMENTS.md)。

### 核心依赖

| 库 | 许可证 | 说明 |
|----|--------|------|
| [Rapier](https://rapier.rs) | Apache-2.0 | 3D 物理引擎 |
| [glam](https://github.com/bitshifter/glam-rs) | MIT/Apache-2.0 | 3D 数学库 |
| [mmd-rs](https://github.com/aankor/mmd-rs) | BSD-2-Clause | MMD 格式解析器 |

### 设计参考

| 项目 | 许可证 | 参考内容 |
|------|--------|----------|
| [KAIMyEntity](https://github.com/kjkjkAIStudio/KAIMyEntity) | MIT | 原始 Minecraft MMD 模组 |
| [KAIMyEntity-C](https://github.com/Gengorou-C/KAIMyEntity-C) | MIT | 本项目的直接前身（二次开发基础） |
| [Saba](https://github.com/benikabocha/saba) | MIT | 物理系统架构 |
| [nphysics](https://github.com/dimforge/nphysics) | Apache-2.0 | 骨骼层次设计 |
| [mdanceio](https://github.com/ReaNAiveD/mdanceio) | MIT | 动画系统 |

完整的第三方许可证信息请参阅 [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md)。

## 相关项目

- [MikuMikuDance](https://sites.google.com/view/vpvp/) - 樋口優开发的原版 MMD 软件
- [Saba](https://github.com/benikabocha/saba) - C++ MMD 库
- [Rapier](https://rapier.rs) - Rust 物理引擎