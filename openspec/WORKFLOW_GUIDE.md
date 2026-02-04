# OpenSpec 工作流程指南

本指南描述 MC-MMD-rust 项目中 OpenSpec 提案的完整工作流程。

## 目录

1. [提案创建流程](#提案创建流程)
2. [提案审核流程](#提案审核流程)
3. [提案归档流程](#提案归档流程)
4. [规范同步流程](#规范同步流程)
5. [提案判定标准](#提案判定标准)
6. [常见场景](#常见场景)

---

## 提案创建流程

### 步骤 1: 确定需要提案

在开始之前,请确认变更需要创建提案。参见[提案判定标准](#提案判定标准)。

**决策树**:
```
新请求?
├─ Bug 修复（恢复规范行为）? → 直接修复
├─ 拼写/格式/注释? → 直接修复
├─ 新功能/能力? → 创建提案
├─ 破坏性变更? → 创建提案
├─ 架构变更? → 创建提案
└─ 不确定? → 创建提案（更安全）
```

### 步骤 2: 探索现有状态

```bash
# 列出现有规范
openspec list --specs

# 列出活跃提案
openspec list

# 查看项目上下文
cat openspec/project.md

# 搜索相关内容（如需要）
rg -n "Requirement:|Scenario:" openspec/specs
```

### 步骤 3: 选择变更 ID

使用以下格式:
- **动词前缀**: `add-`, `update-`, `remove-`, `refactor-`, `fix-`
- **短名称**: kebab-case, 描述性
- **唯一性**: 如果已被使用,追加 `-2`, `-3` 等

**示例**:
- ✅ `add-vmd-animation-layering`
- ✅ `update-physics-engine-to-rapier-0.24`
- ✅ `refactor-jni-memory-management`
- ❌ `feature` (太笼统)
- ❌ `add_stuff` (不描述性)

### 步骤 4: 创建提案目录结构

```bash
CHANGE_ID="add-vmd-animation-layering"

# 创建目录
mkdir -p "openspec/changes/$CHANGE_ID/specs"

# 复制模板
cp openspec/templates/proposal.md.template "openspec/changes/$CHANGE_ID/proposal.md"
cp openspec/templates/tasks.md.template "openspec/changes/$CHANGE_ID/tasks.md"
```

### 步骤 5: 编写提案文档

#### proposal.md 必需章节:

1. **Why**: 1-2 句话描述问题或机会
2. **What Changes**: 变更列表,标记破坏性变更
3. **Impact**:
   - 受影响的规范
   - 受影响的代码

#### 可选章节 (design.md):

仅在以下情况创建 `design.md`:
- 跨多个系统/模块的变更
- 新的架构模式
- 新的外部依赖或重大数据模型变更
- 涉及安全/性能/迁移的复杂性
- 需要 UI/UX 可视化

### 步骤 6: 编写规范增量

为每个受影响的能力创建 `specs/<capability>/spec.md`:

```bash
# 示例: 如果修改动画系统
mkdir -p "openspec/changes/$CHANGE_ID/specs/animation-system"
```

**规范增量格式**:

```markdown
## ADDED Requirements
### Requirement: 新功能名称
The system SHALL [描述]

#### Scenario: 成功案例
- **WHEN** [条件]
- **THEN** [结果]

## MODIFIED Requirements
### Requirement: 现有功能名称
[完整的更新后需求]

## REMOVED Requirements
### Requirement: 旧功能
**Reason**: [原因]
**Migration**: [迁移指南]
```

**重要提示**:
- 场景使用 `#### Scenario:` (4 个 #)
- 每个需求至少一个场景
- MODIFIED 需求要包含完整内容

### 步骤 7: 验证提案

```bash
# 严格验证
openspec validate "$CHANGE_ID" --strict

# 查看详细信息
openspec show "$CHANGE_ID"

# 检查增量
openspec show "$CHANGE_ID" --json --deltas-only
```

修复所有错误后再提交审核。

### 步骤 8: 提交审核

```bash
# 创建分支
git checkout -b "feature/$CHANGE_ID"

# 提交提案
git add "openspec/changes/$CHANGE_ID"
git commit -m "feat: 提案 - $CHANGE_ID"

# 推送到远程
git push origin "feature/$CHANGE_ID"
```

创建 Pull Request 并请求审查。

---

## 提案审核流程

### 审核者检查清单

#### 内容审查

- [ ] **Why 清晰**: 问题或机会是否清楚?
- [ ] **What 完整**: 变更是否列出所有影响?
- [ ] **Impact 准确**: 受影响的代码和规范是否正确?
- [ ] **设计合理** (如有 design.md): 技术决策是否合理?

#### 规范审查

- [ ] **增量格式正确**: ADDED/MODIFIED/REMOVED 标签使用正确
- [ ] **场景完整**: 每个需求至少一个场景
- [ ] **场景格式**: 使用 `#### Scenario:` 格式
- [ ] **SHALL/MUST**: 规范性需求使用正确词汇

#### tasks.md 审查

- [ ] **任务完整**: 覆盖所有实施步骤
- [ ] **顺序合理**: 任务依赖关系清晰
- [ ] **可测试**: 每个任务可验证完成

#### 项目特定检查

- [ ] **JNI 边界**: 如涉及跨语言,是否考虑性能?
- [ ] **性能影响**: 是否说明性能目标?
- [ ] **兼容性**: Fabric/Forge 都考虑了吗?
- [ ] **测试策略**: 是否包含跨语言集成测试?

### 审核决策

**批准**: 所有检查通过
- 可以开始实施

**需要修改**: 部分检查未通过
- 作者修改后重新提交

**拒绝**: 设计不合理或不符合项目方向
- 关闭 PR,讨论后重新提案

---

## 提案归档流程

归档发生在代码合并并部署之后。

### 时机

- ✅ 代码已合并到 main 分支
- ✅ 功能已部署/发布
- ✅ 所有测试通过
- ❌ 不要在代码合并前归档

### 自动归档（推荐）

```bash
# 确保在 main 分支
git checkout main
git pull

# 归档提案
openspec archive "$CHANGE_ID" --yes

# 查看归档结果
openspec list
openspec show "$CHANGE_ID"
```

### 手动归档（可选）

如果需要手动归档（如自动归档失败）:

```bash
# 1. 移动提案到 archive 目录
ARCHIVE_DATE=$(date +%Y-%m-%d)
mv "openspec/changes/$CHANGE_ID" "openspec/changes/archive/$ARCHIVE_DATE-$CHANGE_ID"

# 2. 更新 specs/ 目录
# 手动合并增量到规范文件

# 3. 验证
openspec validate --strict
```

### 归档后验证

```bash
# 检查规范已更新
openspec show <capability> --type spec

# 检查提案已归档
openspec list

# 确认无错误
openspec validate --strict
```

---

## 规范同步流程

归档提案时,OpenSpec 会自动合并增量到主规范。

### 自动合并

```bash
# 标准归档会自动更新规范
openspec archive "$CHANGE_ID"
```

**合并规则**:
- **ADDED**: 添加新需求到规范
- **MODIFIED**: 替换现有需求
- **REMOVED**: 删除需求并保留注释

### 仅工具变更（无规范变更）

```bash
# 仅归档提案,不更新规范
openspec archive "$CHANGE_ID" --skip-specs
```

使用场景:
- 文档更新
- 构建系统变更
- 测试代码添加

### 手动合并冲突

如果自动合并出现冲突:

1. 查看冲突: `openspec validate --strict`
2. 手动编辑 `openspec/specs/<capability>/spec.md`
3. 解决冲突
4. 验证: `openspec validate <spec-id> --strict`

---

## 提案判定标准

### 需要提案的变更

#### 新功能或能力

- ✅ 添加新的动画格式支持 (如 `add-vmd-animation-support`)
- ✅ 添加新的渲染特性 (如 `add-ray-tracing-support`)
- ✅ 添加新的物理特性 (如 `add-cloth-simulation`)

#### 破坏性变更

- ✅ 修改 JNI 接口 (如 `update-jni-model-interface`)
- ✅ 修改数据格式 (如 `update-pmx-internal-structure`)
- ✅ 移除公开 API (如 `remove-deprecated-animation-api`)

#### 跨边界的变更

- ✅ Rust-Java 接口修改 (如 `refactor-jni-boundary`)
- ✅ 物理数据传输格式变更 (如 `update-physics-data-layout`)

#### 性能优化（改变行为）

- ✅ 改变物理算法 (如 `update-physics-solver`)
- ✅ 改变渲染管线 (如 `implement-compute-shader-skinning`)

#### 架构变更

- ✅ 引入新的架构模式 (如 `add-component-based-architecture`)
- ✅ 重大重构 (如 `refactor-animation-system`)

### 无需提案的变更

#### Bug 修复

- ✅ 恢复预期行为的修复
- ❌ 不改变行为的"优化"

#### 文档和格式

- ✅ 拼写错误修正
- ✅ 代码格式化
- ✅ 注释改进
- ✅ README 更新

#### 依赖更新

- ✅ 非破坏性依赖更新
- ❌ 破坏性依赖更新需要提案

#### 测试代码

- ✅ 添加测试用例
- ✅ 改进测试覆盖

#### 配置变更

- ✅ 构建配置调整
- ✅ CI/CD 流程改进

### 边界情况

**不确定时,创建提案**。这更安全且有助于文档化。

---

## 常见场景

### 场景 1: 添加新的动画格式

```bash
# 1. 探索
openspec list --specs
# 确认没有现有提案

# 2. 创建提案
CHANGE="add-vmd-animation-support"
mkdir -p "openspec/changes/$CHANGE/specs/animation-system"

# 3. 编写 proposal.md
# - Why: 支持更丰富的动画
# - What: 添加 VMD 解析器,播放器
# - Impact: animation-system 能力

# 4. 编写 spec 增量
# - ADDED: VMD 加载需求
# - ADDED: VMD 播放需求

# 5. 验证
openspec validate "$CHANGE" --strict
```

### 场景 2: 优化 JNI 性能

```bash
# 1. 分析瓶颈
# 确定 JNI 调用是瓶颈

# 2. 创建提案
CHANGE="refactor-jni-bulk-data-transfer"
mkdir -p "openspec/changes/$CHANGE/specs/jni-bridge"

# 3. 编写提案
# - Why: JNI 频繁调用导致性能下降
# - What: 批量传输数据,减少调用次数
# - Impact: jni-bridge 能力 (MODIFIED)

# 4. 编写 tasks.md
# 添加性能测试任务

# 5. 验证
openspec validate "$CHANGE" --strict
```

### 场景 3: 修复 Bug

```bash
# 直接修复,无需提案
git checkout -b "fix/physics-crash"
# ... 修复代码 ...
git commit -m "fix: 修复物理引擎空指针崩溃"
git push origin fix/physics-crash
# 创建 PR
```

### 场景 4: 更新依赖版本

```bash
# 非破坏性更新: 直接修改
# 编辑 Cargo.toml 或 build.gradle

# 破坏性更新: 需要提案
CHANGE="update-rapier-to-0.24"
mkdir -p "openspec/changes/$CHANGE/specs/physics-engine"
# 编写提案说明 API 变更和迁移指南
```

---

## 快速参考

### 常用命令

```bash
# 列出活跃提案
openspec list

# 列出规范
openspec list --specs

# 查看详情
openspec show <change-id>
openspec show <spec-id> --type spec

# 验证
openspec validate <change-id> --strict
openspec validate --strict

# 归档
openspec archive <change-id> --yes
openspec archive <change-id> --skip-specs --yes
```

### 工作流程检查清单

**提案前**:
- [ ] 确认需要提案
- [ ] 搜索现有规范和提案
- [ ] 选择唯一的 change-id

**提案中**:
- [ ] 编写 proposal.md
- [ ] 编写 tasks.md
- [ ] 编写 spec 增量 (如需要)
- [ ] 编写 design.md (如需要)
- [ ] 验证通过

**实施前**:
- [ ] 提案审核通过
- [ ] 创建 feature 分支
- [ ] 按 tasks.md 实施

**实施后**:
- [ ] 所有测试通过
- [ ] 代码审查通过
- [ ] 合并到 main

**部署后**:
- [ ] 归档提案
- [ ] 验证规范已更新
