# proposal-management Specification

## Purpose
TBD - created by archiving change openspec-proposal-management-initialization. Update Purpose after archive.
## Requirements
### Requirement: Proposal Management System
项目 SHALL 提供完整的OpenSpec提案管理系统,用于管理技术变更提案、规范文档和架构决策。

#### Scenario: 初始化提案系统
- **GIVEN** 项目处于初始阶段
- **WHEN** 执行初始化提案
- **THEN** 系统创建完整的OpenSpec目录结构
- **AND** 生成项目上下文文档(project.md)
- **AND** 配置AI代理使用指南(AGENTS.md)

#### Scenario: 创建变更提案
- **GIVEN** 开发者需要实现新功能或进行破坏性变更
- **WHEN** 开发者通过AI助手创建提案
- **THEN** 系统生成标准化的提案文件(proposal.md, tasks.md)
- **AND** 如需要,生成design.md设计文档
- **AND** 创建规范增量文件(specs/**/*.md)

#### Scenario: 提案验证
- **GIVEN** 提案文件已创建
- **WHEN** 执行 `openspec validate <change-id> --strict`
- **THEN** 系统验证提案结构和格式
- **AND** 检查所有必需的Scenario是否存在
- **AND** 确认规范增量格式正确

#### Scenario: 提案归档
- **GIVEN** 提案已实施并合并
- **WHEN** 执行 `openspec archive <change-id>`
- **THEN** 系统将提案移至archive目录
- **AND** 更新specs目录中的规范文档
- **AND** 保留完整的技术决策历史

### Requirement: Project Context Documentation
project.md SHALL 包含完整的项目上下文信息,包括项目目的、技术栈、代码风格、架构模式等。

#### Scenario: 项目目的定义
- **GIVEN** project.md文档存在
- **WHEN** 查看Purpose章节
- **THEN** 清晰描述项目的核心目标(在Minecraft中实现MMD渲染和物理模拟)
- **AND** 说明项目的关键价值主张

#### Scenario: 技术栈记录
- **GIVEN** project.md文档存在
- **WHEN** 查看Tech Stack章节
- **THEN** 列出所有主要技术(Rust 1.70+, Java 17+, Gradle)
- **AND** 记录关键依赖(Rapier3D, OpenGL, mmd-rs)

#### Scenario: 代码风格约定
- **GIVEN** project.md文档存在
- **WHEN** 查看Code Style章节
- **THEN** 定义Rust代码风格(rustfmt, 2018 edition)
- **AND** 定义Java代码风格(Google Style, 4 spaces)

#### Scenario: 架构模式文档化
- **GIVEN** project.md文档存在
- **WHEN** 查看Architecture Patterns章节
- **THEN** 说明JNI边界分离策略
- **AND** 记录Rust负责物理/动画,Java负责渲染的职责划分

#### Scenario: 测试策略定义
- **GIVEN** project.md文档存在
- **WHEN** 查看Testing Strategy章节
- **THEN** 说明Rust单元测试策略(cargo test)
- **AND** 说明Java集成测试策略(JUnit + Minecraft测试模组)

### Requirement: AI Agent Instructions
AGENTS.md SHALL 为AI编码助手提供清晰的工作流程指导,包括提案创建、实施和归档的完整说明。

#### Scenario: 提案创建指导
- **GIVEN** AI助手接收到创建提案的请求
- **WHEN** 读取AGENTS.md中的Stage 1: Creating Changes
- **THEN** AI了解何时需要创建提案
- **AND** 知道如何生成标准化的提案文件

#### Scenario: 提案实施指导
- **GIVEN** AI助手接收到实施变更的请求
- **WHEN** 读取AGENTS.md中的Stage 2: Implementing Changes
- **THEN** AI按tasks.md顺序执行实施
- **AND** 确保每个任务完成后再继续

#### Scenario: 提案归档指导
- **GIVEN** 提案已实施完成
- **WHEN** 读取AGENTS.md中的Stage 3: Archiving Changes
- **THEN** AI执行归档流程
- **AND** 更新specs目录

### Requirement: Proposal Creation Workflow
系统 SHALL 定义清晰的提案创建工作流程,包括需求分析、文件生成和验证步骤。

#### Scenario: 需要提案的变更
- **GIVEN** 开发者计划进行变更
- **WHEN** 变更属于以下类型之一:新增功能、破坏性变更、架构变更、性能优化
- **THEN** MUST创建OpenSpec提案

#### Scenario: 不需要提案的变更
- **GIVEN** 开发者计划进行变更
- **WHEN** 变更属于以下类型之一:Bug修复、拼写错误、格式化、注释
- **THEN** MAY直接修改代码,无需提案

#### Scenario: 提案文件生成
- **GIVEN** 决定创建提案
- **WHEN** 选择唯一的change-id(kebab-case, verb-led)
- **THEN** 系统创建changes/<change-id>/目录
- **AND** 生成proposal.md文件
- **AND** 生成tasks.md文件
- **AND** 根据需要生成design.md文件
- **AND** 创建specs/<capability>/spec.md增量文件

### Requirement: Proposal Validation
系统 SHALL 提供提案验证功能,确保提案符合OpenSpec规范和项目要求。

#### Scenario: 严格验证
- **GIVEN** 提案已创建
- **WHEN** 执行 `openspec validate <change-id> --strict`
- **THEN** 系统检查提案目录结构
- **AND** 验证所有必需文件存在
- **AND** 检查Scenario格式(#### Scenario:)
- **AND** 确认规范增量格式(## ADDED/MODIFIED/REMOVED Requirements)

#### Scenario: 验证失败处理
- **GIVEN** 提案验证失败
- **WHEN** 收到验证错误信息
- **THEN** 系统明确指出错误位置
- **AND** 提供修复建议
- **AND** 开发者修复后重新验证

### Requirement: Design Documentation Criteria
系统 SHALL 定义明确的design.md创建标准,仅在需要额外设计说明时创建。

#### Scenario: 需要design.md的情况
- **GIVEN** 正在创建提案
- **WHEN** 变更满足以下任一条件:
  - 跨多个系统/模块
  - 引入新的架构模式
  - 新的外部依赖或重大数据模型变更
  - 安全/性能/迁移的复杂性
  - 需要可视化设计(UI/数据流/架构)
- **THEN** MUST创建design.md

#### Scenario: 不需要design.md的情况
- **GIVEN** 正在创建提案
- **WHEN** 变更是简单直接的功能添加
- **AND** 不涉及跨系统协调
- **AND** 无需复杂的架构决策
- **THEN** MAY省略design.md

### Requirement: Specification Organization
specs/目录 SHALL 按能力(capability)组织,每个能力对应一个独立的功能领域。

#### Scenario: 能力目录创建
- **GIVEN** 需要为新能力创建规范
- **WHEN** 确定能力的kebab-case名称
- **THEN** 在specs/下创建<capability>/目录
- **AND** 创建spec.md文件
- **AND** 按需创建design.md文件

#### Scenario: 预定义能力
- **GIVEN** 项目初始阶段
- **WHEN** 建立规范结构
- **THEN** 创建以下能力目录:
  - `pmx-model-loading/` - PMX模型加载
  - `vmd-animation/` - VMD动画播放
  - `physics-simulation/` - 物理模拟
  - `gpu-skinning/` - GPU蒙皮
  - `jni-bridge/` - Rust-Java桥接

### Requirement: Delta Specification Format
规范增量文件 SHALL 使用标准的ADDED/MODIFIED/REMOVED格式,确保规范变更的清晰可追溯。

#### Scenario: ADDED Requirements
- **GIVEN** 引入新能力
- **WHEN** 创建规范增量
- **THEN** 使用 `## ADDED Requirements` 标题
- **AND** 每个Requirement使用 `### Requirement: 名称` 格式
- **AND** 每个Requirement至少有一个 `#### Scenario:`

#### Scenario: MODIFIED Requirements
- **GIVEN** 修改现有能力
- **WHEN** 创建规范增量
- **THEN** 使用 `## MODIFIED Requirements` 标题
- **AND** 包含完整的修改后的Requirement内容
- **AND** 保留所有相关的Scenario

#### Scenario: REMOVED Requirements
- **GIVEN** 移除旧功能
- **WHEN** 创建规范增量
- **THEN** 使用 `## REMOVED Requirements` 标题
- **AND** 说明移除原因
- **AND** 提供迁移指导

### Requirement: Multi-Language Development Support
系统 SHALL 支持Rust-Java跨语言开发的项目特定工作流。

#### Scenario: JNI边界变更提案
- **GIVEN** 需要修改Rust-Java接口
- **WHEN** 创建提案
- **THEN** proposal.md说明JNI边界的变更
- **AND** design.md包含数据流图
- **AND** tasks.md包含FFI安全性验证

#### Scenario: 跨语言测试指导
- **GIVEN** 提案涉及跨语言功能
- **WHEN** 编写tasks.md
- **THEN** 包含Rust单元测试任务
- **AND** 包含Java集成测试任务
- **AND** 包含跨语言边界测试

### Requirement: Performance Considerations
系统 SHALL 在提案中考虑性能影响,特别是物理模拟和渲染相关的变更。

#### Scenario: 性能影响评估
- **GIVEN** 提案可能影响性能
- **WHEN** 编写proposal.md
- **THEN** 在Impact章节评估性能影响
- **AND** 如相关,说明对60FPS目标的影响

#### Scenario: 性能测试要求
- **GIVEN** 提案涉及物理或渲染
- **WHEN** 编写tasks.md
- **THEN** 包含性能基准测试
- **AND** 包含内存使用评估

