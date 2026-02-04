# 规范增量: MMD 渲染系统

## ADDED Requirements

### Requirement: 骨骼可见性控制

The system SHALL provide the ability to control the visibility of individual bones for debugging and visualization purposes.

#### Scenario: 设置单个骨骼可见性

- **GIVEN** 一个已加载的 MMD 模型
- **WHEN** 调用 `setBoneVisibility(boneIndex, true)`
- **THEN** 指定骨骼在调试渲染中可见
- **AND** 其他骨骼的可见性不受影响

#### Scenario: 批量设置骨骼可见性

- **GIVEN** 一个已加载的 MMD 模型
- **WHEN** 调用 `setAllBonesVisibility(true)`
- **THEN** 所有骨骼在调试渲染中可见

#### Scenario: 获取骨骼可见性状态

- **GIVEN** 一个已加载的 MMD 模型
- **WHEN** 调用 `getBoneVisibility(boneIndex)`
- **THEN** 返回该骨骼的当前可见性状态
- **AND** 默认返回 `false`（不可见）

#### Scenario: 可见性不影响模型渲染

- **GIVEN** 一个已加载的 MMD 模型
- **WHEN** 设置骨骼可见性
- **THEN** 正常的模型渲染不受影响
- **AND** 仅调试骨骼绘制受影响
