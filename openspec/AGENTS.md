# OpenSpec Instructions

Instructions for AI coding assistants using OpenSpec for spec-driven development.

## TL;DR Quick Checklist

- Search existing work: `openspec spec list --long`, `openspec list` (use `rg` only for full-text search)
- Decide scope: new capability vs modify existing capability
- Pick a unique `change-id`: kebab-case, verb-led (`add-`, `update-`, `remove-`, `refactor-`)
- Scaffold: `proposal.md`, `tasks.md`, `design.md` (only if needed), and delta specs per affected capability
- Write deltas: use `## ADDED|MODIFIED|REMOVED|RENAMED Requirements`; include at least one `#### Scenario:` per requirement
- Validate: `openspec validate [change-id] --strict` and fix issues
- Request approval: Do not start implementation until proposal is approved

## Three-Stage Workflow

### Stage 1: Creating Changes
Create proposal when you need to:
- Add features or functionality
- Make breaking changes (API, schema)
- Change architecture or patterns  
- Optimize performance (changes behavior)
- Update security patterns

Triggers (examples):
- "Help me create a change proposal"
- "Help me plan a change"
- "Help me create a proposal"
- "I want to create a spec proposal"
- "I want to create a spec"

Loose matching guidance:
- Contains one of: `proposal`, `change`, `spec`
- With one of: `create`, `plan`, `make`, `start`, `help`

Skip proposal for:
- Bug fixes (restore intended behavior)
- Typos, formatting, comments
- Dependency updates (non-breaking)
- Configuration changes
- Tests for existing behavior

**Workflow**
1. Review `openspec/project.md`, `openspec list`, and `openspec list --specs` to understand current context.
2. Choose a unique verb-led `change-id` and scaffold `proposal.md`, `tasks.md`, optional `design.md`, and spec deltas under `openspec/changes/<id>/`.
3. Draft spec deltas using `## ADDED|MODIFIED|REMOVED Requirements` with at least one `#### Scenario:` per requirement.
4. Run `openspec validate <id> --strict` and resolve any issues before sharing the proposal.

### Stage 2: Implementing Changes
Track these steps as TODOs and complete them one by one.
1. **Read proposal.md** - Understand what's being built
2. **Read design.md** (if exists) - Review technical decisions
3. **Read tasks.md** - Get implementation checklist
4. **Implement tasks sequentially** - Complete in order
5. **Confirm completion** - Ensure every item in `tasks.md` is finished before updating statuses
6. **Update checklist** - After all work is done, set every task to `- [x]` so the list reflects reality
7. **Approval gate** - Do not start implementation until the proposal is reviewed and approved

### Stage 3: Archiving Changes
After deployment, create separate PR to:
- Move `changes/[name]/` → `changes/archive/YYYY-MM-DD-[name]/`
- Update `specs/` if capabilities changed
- Use `openspec archive <change-id> --skip-specs --yes` for tooling-only changes (always pass the change ID explicitly)
- Run `openspec validate --strict` to confirm the archived change passes checks

## Before Any Task

**Context Checklist:**
- [ ] Read relevant specs in `specs/[capability]/spec.md`
- [ ] Check pending changes in `changes/` for conflicts
- [ ] Read `openspec/project.md` for conventions
- [ ] Run `openspec list` to see active changes
- [ ] Run `openspec list --specs` to see existing capabilities

**Before Creating Specs:**
- Always check if capability already exists
- Prefer modifying existing specs over creating duplicates
- Use `openspec show [spec]` to review current state
- If request is ambiguous, ask 1–2 clarifying questions before scaffolding

### Search Guidance
- Enumerate specs: `openspec spec list --long` (or `--json` for scripts)
- Enumerate changes: `openspec list` (or `openspec change list --json` - deprecated but available)
- Show details:
  - Spec: `openspec show <spec-id> --type spec` (use `--json` for filters)
  - Change: `openspec show <change-id> --json --deltas-only`
- Full-text search (use ripgrep): `rg -n "Requirement:|Scenario:" openspec/specs`

## Quick Start

### CLI Commands

```bash
# Essential commands
openspec list                  # List active changes
openspec list --specs          # List specifications
openspec show [item]           # Display change or spec
openspec validate [item]       # Validate changes or specs
openspec archive <change-id> [--yes|-y]   # Archive after deployment (add --yes for non-interactive runs)

# Project management
openspec init [path]           # Initialize OpenSpec
openspec update [path]         # Update instruction files

# Interactive mode
openspec show                  # Prompts for selection
openspec validate              # Bulk validation mode

# Debugging
openspec show [change] --json --deltas-only
openspec validate [change] --strict
```

### Command Flags

- `--json` - Machine-readable output
- `--type change|spec` - Disambiguate items
- `--strict` - Comprehensive validation
- `--no-interactive` - Disable prompts
- `--skip-specs` - Archive without spec updates
- `--yes`/`-y` - Skip confirmation prompts (non-interactive archive)

## Directory Structure

```
openspec/
├── project.md              # Project conventions
├── specs/                  # Current truth - what IS built
│   └── [capability]/       # Single focused capability
│       ├── spec.md         # Requirements and scenarios
│       └── design.md       # Technical patterns
├── changes/                # Proposals - what SHOULD change
│   ├── [change-name]/
│   │   ├── proposal.md     # Why, what, impact
│   │   ├── tasks.md        # Implementation checklist
│   │   ├── design.md       # Technical decisions (optional; see criteria)
│   │   └── specs/          # Delta changes
│   │       └── [capability]/
│   │           └── spec.md # ADDED/MODIFIED/REMOVED
│   └── archive/            # Completed changes
```

## Creating Change Proposals

### Decision Tree

```
New request?
├─ Bug fix restoring spec behavior? → Fix directly
├─ Typo/format/comment? → Fix directly  
├─ New feature/capability? → Create proposal
├─ Breaking change? → Create proposal
├─ Architecture change? → Create proposal
└─ Unclear? → Create proposal (safer)
```

### Proposal Structure

1. **Create directory:** `changes/[change-id]/` (kebab-case, verb-led, unique)

2. **Write proposal.md:**
```markdown
# Change: [Brief description of change]

## Why
[1-2 sentences on problem/opportunity]

## What Changes
- [Bullet list of changes]
- [Mark breaking changes with **BREAKING**]

## UI Design Changes (if applicable)
- Include ASCII mockups for new/modified interfaces
- Add Mermaid sequence diagrams for user interaction flows
- Reference: PROPOSAL_DESIGN_GUIDELINES.md for format requirements

## Code Flow Changes (if applicable)
- Include Mermaid flowcharts for data flow
- Add sequence diagrams for API interactions
- Include architecture diagrams for system changes
- Reference: PROPOSAL_DESIGN_GUIDELINES.md for format requirements

## Impact
- Affected specs: [list capabilities]
- Affected code: [key files/systems]
```

3. **Create spec deltas:** `specs/[capability]/spec.md`
```markdown
## ADDED Requirements
### Requirement: New Feature
The system SHALL provide...

#### Scenario: Success case
- **WHEN** user performs action
- **THEN** expected result

## MODIFIED Requirements
### Requirement: Existing Feature
[Complete modified requirement]

## REMOVED Requirements
### Requirement: Old Feature
**Reason**: [Why removing]
**Migration**: [How to handle]
```
If multiple capabilities are affected, create multiple delta files under `changes/[change-id]/specs/<capability>/spec.md`—one per capability.

4. **Create tasks.md:**
```markdown
## 1. Implementation
- [ ] 1.1 Create database schema
- [ ] 1.2 Implement API endpoint
- [ ] 1.3 Add frontend component
- [ ] 1.4 Write tests
```

5. **Create design.md when needed:**
Create `design.md` if any of the following apply; otherwise omit it:
- Cross-cutting change (multiple services/modules) or a new architectural pattern
- New external dependency or significant data model changes
- Security, performance, or migration complexity
- Ambiguity that benefits from technical decisions before coding
- UI/UX changes requiring visual mockups and interaction flows
- Code flow changes requiring diagrams (sequence, flowchart, architecture)

**Note**: For design guidelines including UI mockups and code flow diagrams, see [PROPOSAL_DESIGN_GUIDELINES.md](PROPOSAL_DESIGN_GUIDELINES.md)

Minimal `design.md` skeleton:
```markdown
## Context
[Background, constraints, stakeholders]

## Goals / Non-Goals
- Goals: [...]
- Non-Goals: [...]

## Decisions
- Decision: [What and why]
- Alternatives considered: [Options + rationale]

## UI/UX Design
[Include if UI changes are involved]
- ASCII mockups of new/modified interfaces
- User interaction flows (Mermaid sequence diagrams)
- State transitions and error handling UI
- Mobile/responsive considerations
- See PROPOSAL_DESIGN_GUIDELINES.md for detailed requirements

## Technical Design
[Include if code flow changes are involved]
- Architecture diagrams (Mermaid)
- Data flow diagrams (Mermaid flowcharts)
- API interaction sequences (Mermaid sequence diagrams)
- Component relationships (Mermaid graphs)
- See PROPOSAL_DESIGN_GUIDELINES.md for detailed requirements

## Risks / Trade-offs
- [Risk] → Mitigation

## Migration Plan
[Steps, rollback]

## Open Questions
- [...]
```

## Spec File Format

### Critical: Scenario Formatting

**CORRECT** (use #### headers):
```markdown
#### Scenario: User login success
- **WHEN** valid credentials provided
- **THEN** return JWT token
```

**WRONG** (don't use bullets or bold):
```markdown
- **Scenario: User login**  ❌
**Scenario**: User login     ❌
### Scenario: User login      ❌
```

Every requirement MUST have at least one scenario.

### Requirement Wording
- Use SHALL/MUST for normative requirements (avoid should/may unless intentionally non-normative)

### Delta Operations

- `## ADDED Requirements` - New capabilities
- `## MODIFIED Requirements` - Changed behavior
- `## REMOVED Requirements` - Deprecated features
- `## RENAMED Requirements` - Name changes

Headers matched with `trim(header)` - whitespace ignored.

#### When to use ADDED vs MODIFIED
- ADDED: Introduces a new capability or sub-capability that can stand alone as a requirement. Prefer ADDED when the change is orthogonal (e.g., adding "Slash Command Configuration") rather than altering the semantics of an existing requirement.
- MODIFIED: Changes the behavior, scope, or acceptance criteria of an existing requirement. Always paste the full, updated requirement content (header + all scenarios). The archiver will replace the entire requirement with what you provide here; partial deltas will drop previous details.
- RENAMED: Use when only the name changes. If you also change behavior, use RENAMED (name) plus MODIFIED (content) referencing the new name.

Common pitfall: Using MODIFIED to add a new concern without including the previous text. This causes loss of detail at archive time. If you aren’t explicitly changing the existing requirement, add a new requirement under ADDED instead.

Authoring a MODIFIED requirement correctly:
1) Locate the existing requirement in `openspec/specs/<capability>/spec.md`.
2) Copy the entire requirement block (from `### Requirement: ...` through its scenarios).
3) Paste it under `## MODIFIED Requirements` and edit to reflect the new behavior.
4) Ensure the header text matches exactly (whitespace-insensitive) and keep at least one `#### Scenario:`.

Example for RENAMED:
```markdown
## RENAMED Requirements
- FROM: `### Requirement: Login`
- TO: `### Requirement: User Authentication`
```

## Troubleshooting

### Common Errors

**"Change must have at least one delta"**
- Check `changes/[name]/specs/` exists with .md files
- Verify files have operation prefixes (## ADDED Requirements)

**"Requirement must have at least one scenario"**
- Check scenarios use `#### Scenario:` format (4 hashtags)
- Don't use bullet points or bold for scenario headers

**Silent scenario parsing failures**
- Exact format required: `#### Scenario: Name`
- Debug with: `openspec show [change] --json --deltas-only`

### Validation Tips

```bash
# Always use strict mode for comprehensive checks
openspec validate [change] --strict

# Debug delta parsing
openspec show [change] --json | jq '.deltas'

# Check specific requirement
openspec show [spec] --json -r 1
```

## Happy Path Script

```bash
# 1) Explore current state
openspec spec list --long
openspec list
# Optional full-text search:
# rg -n "Requirement:|Scenario:" openspec/specs
# rg -n "^#|Requirement:" openspec/changes

# 2) Choose change id and scaffold
CHANGE=add-two-factor-auth
mkdir -p openspec/changes/$CHANGE/{specs/auth}
printf "## Why\n...\n\n## What Changes\n- ...\n\n## Impact\n- ...\n" > openspec/changes/$CHANGE/proposal.md
printf "## 1. Implementation\n- [ ] 1.1 ...\n" > openspec/changes/$CHANGE/tasks.md

# 3) Add deltas (example)
cat > openspec/changes/$CHANGE/specs/auth/spec.md << 'EOF'
## ADDED Requirements
### Requirement: Two-Factor Authentication
Users MUST provide a second factor during login.

#### Scenario: OTP required
- **WHEN** valid credentials are provided
- **THEN** an OTP challenge is required
EOF

# 4) Validate
openspec validate $CHANGE --strict
```

## Multi-Capability Example

```
openspec/changes/add-2fa-notify/
├── proposal.md
├── tasks.md
└── specs/
    ├── auth/
    │   └── spec.md   # ADDED: Two-Factor Authentication
    └── notifications/
        └── spec.md   # ADDED: OTP email notification
```

auth/spec.md
```markdown
## ADDED Requirements
### Requirement: Two-Factor Authentication
...
```

notifications/spec.md
```markdown
## ADDED Requirements
### Requirement: OTP Email Notification
...
```

## Best Practices

### Design Visualization
- **UI Changes**: Always include ASCII mockups and interaction flows (see [PROPOSAL_DESIGN_GUIDELINES.md](PROPOSAL_DESIGN_GUIDELINES.md))
- **Code Changes**: Include Mermaid diagrams for data flows, sequences, and architecture
- **Error Paths**: Document error handling flows in both UI and code diagrams
- **State Transitions**: Show before/after states clearly in diagrams

### Simplicity First
- Default to <100 lines of new code
- Single-file implementations until proven insufficient
- Avoid frameworks without clear justification
- Choose boring, proven patterns

### Complexity Triggers
Only add complexity with:
- Performance data showing current solution too slow
- Concrete scale requirements (>1000 users, >100MB data)
- Multiple proven use cases requiring abstraction

### Clear References
- Use `file.ts:42` format for code locations
- Reference specs as `specs/auth/spec.md`
- Link related changes and PRs

### Capability Naming
- Use verb-noun: `user-auth`, `payment-capture`
- Single purpose per capability
- 10-minute understandability rule
- Split if description needs "AND"

### Change ID Naming
- Use kebab-case, short and descriptive: `add-two-factor-auth`
- Prefer verb-led prefixes: `add-`, `update-`, `remove-`, `refactor-`
- Ensure uniqueness; if taken, append `-2`, `-3`, etc.

## Tool Selection Guide

| Task | Tool | Why |
|------|------|-----|
| Find files by pattern | Glob | Fast pattern matching |
| Search code content | Grep | Optimized regex search |
| Read specific files | Read | Direct file access |
| Explore unknown scope | Task | Multi-step investigation |

## Error Recovery

### Change Conflicts
1. Run `openspec list` to see active changes
2. Check for overlapping specs
3. Coordinate with change owners
4. Consider combining proposals

### Validation Failures
1. Run with `--strict` flag
2. Check JSON output for details
3. Verify spec file format
4. Ensure scenarios properly formatted

### Missing Context
1. Read project.md first
2. Check related specs
3. Review recent archives
4. Ask for clarification

## Quick Reference

### Stage Indicators
- `changes/` - Proposed, not yet built
- `specs/` - Built and deployed
- `archive/` - Completed changes

### File Purposes
- `proposal.md` - Why and what
- `tasks.md` - Implementation steps
- `design.md` - Technical decisions
- `spec.md` - Requirements and behavior

### CLI Essentials
```bash
openspec list              # What's in progress?
openspec show [item]       # View details
openspec validate --strict # Is it correct?
openspec archive <change-id> [--yes|-y]  # Mark complete (add --yes for automation)
```

Remember: Specs are truth. Changes are proposals. Keep them in sync.

---

# MC-MMD-rust 项目特定补充

本章节包含 MC-MMD-rust 项目特有的指导,用于补充通用 OpenSpec 工作流程。

## 领域知识速查

### MMD 核心概念

**PMX 模型格式**:
- **骨骼（Bones）**: 层次化结构,支持 IK 链、旋转/移动继承
- **顶点（Vertices）**: 支持 Biped（2骨骼）和 Quad（4骨骼）权重蒙皮
- **材质（Materials）**: Toon 着色、环境光、自发光、边缘绘制
- **变形（Morphs）**: 顶点偏移、UV 变换、材质属性变化
- **刚体（Rigid Bodies）**: 物理引擎中的碰撞体
- **关节（Joints）**: 连接刚体的约束（弹簧、铰链、滑动等）

**VMD 动画格式**:
- **骨骼关键帧**: 位置 + 旋转（四元数）+ 贝塞尔插值曲线
- **表情关键帧**: 变形权重随时间变化
- **插值**: X/Y/Y 轴独立的贝塞尔曲线控制

**物理模拟**:
- **刚体类型**: Static（静态）, Dynamic（动态）, Kinematic（运动学）
- **碰撞形状**: Box, Sphere, Capsule, Cylinder 等
- **物理约束**: Spring, Hinge, Slider, Universal, Cone Twist

**IK 求解**:
- **CC-IK**: 循环坐标下降算法,用于反向运动学
- **IK 链**: 从末端骨骼到 IK 目标的骨骼链
- **约束**: 角度限制、轴限制

## 跨语言开发注意事项

### Rust-Java 边界设计

**JNI 接口原则**:
1. **最小化跨边界调用**: 每次调用都有开销,批量处理数据
2. **使用原始数组**: 传输 `float[]`, `int[]` 而非对象数组
3. **直接缓冲区**: 对于大型数据使用 `ByteBuffer.allocateDirect()`
4. **生命周期管理**: Rust 端管理对象生命周期,Java 仅持有句柄

**常见模式**:

```java
// Java 端示例
public class MMDModel {
    private long nativeHandle; // Rust 对象句柄

    // 批量获取骨骼变换
    public native void getBoneTransforms(long handle, float[] transforms);

    // 一次性更新所有数据
    public native void updateModel(long handle, float deltaTime);
}
```

```rust
// Rust 端示例
#[no_mangle]
pub extern "system" fn Java_com_example_MMDModel_getBoneTransforms(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    transforms: JFloatArray,
) {
    // 从句柄获取模型
    let model = unsafe { &mut *(handle as *mut Model) };

    // 批量写入到 Java 数组
    let mut buf = [0f32; MAX_BONES * 8];
    model.fill_bone_transforms(&mut buf);

    // 一次性传输回 Java
    env.set_float_array_region(transforms, 0, &buf).unwrap();
}
```

**避免的反模式**:
- ❌ 频繁调用单个骨骼的 getter/setter
- ❌ 在 Java 和 Rust 之间传输复杂对象结构
- ❌ 在热循环中跨越 JNI 边界

### 内存安全

**Rust 端**:
- 使用 `unsafe` 块时必须仔细验证
- JNI 函数中使用 `panic::catch_unwind` 防止崩溃传播到 Java
- 使用 `thiserror` 定义清晰的错误类型

**Java 端**:
- 始终在 `try-finally` 或 `try-with-resources` 中释放 native 资源
- 使用 `Cleaner` 或 `PhantomReference` 处理异常情况下的清理

### 并发和线程

**线程模型**:
- **主线程**: Minecraft 游戏循环、渲染
- **物理线程**: Rust 物理模拟（Rapier）
- **动画线程**: 可选的动画混合计算

**同步**:
- 使用 `Mutex` 或 `RwLock` 保护共享状态
- 避免在 JNI 调用中持有锁（可能死锁）
- 使用原子操作进行简单状态同步

## 性能关键路径

**必须保持高性能的区域**:

1. **JNI 边界**: 批量传输,减少调用次数
2. **物理循环**: Rapier step 时间 < 5ms（60 FPS）
3. **蒙皮计算**: GPU Compute Shader 或并行 CPU
4. **IK 求解**: 迭代次数和链长度优化
5. **动画混合**: 使用 SIMD 或 GPU 加速

**性能测试**:
- 使用 Criterion 进行 Rust 基准测试
- 使用 JMH 进行 Java 微基准测试
- 在完整游戏环境中进行端到端测试

## 文件组织

**Rust 代码结构**:
```
rust_engine/src/
├── animation/    # VMD 加载、插值、混合
├── model/        # PMX 加载、运行时模型
├── morph/        # 表情变形管理
├── physics/      # Rapier 物理引擎集成
├── skeleton/     # 骨骼层次、IK 求解
├── skinning/     # 顶点蒙皮计算
├── texture/      # 纹理加载和处理
└── jni_bridge/   # Java 绑定
```

**Java 代码结构**:
```
fabric/forge/src/main/java/.../
├── renderer/     # OpenGL 渲染器
├── compute/      # Compute Shader 管理
├── model/        # MMD 模型实例
├── animation/    # 动画控制
├── config/       # 配置和设置
└── util/         # 工具类
```

## 特定场景指导

### 添加新的 MMD 格式支持

1. 在 `rust_engine/src/model/` 添加解析器
2. 通过 JNI 暴露必要接口
3. 在 Java 端创建对应的加载器
4. 添加测试文件到 `rust_engine/tests/`
5. 创建 OpenSpec 提案（新能力）

### 修改物理模拟参数

1. 在 `rust_engine/src/physics/config.rs` 调整参数
2. 更新相关测试
3. 如果影响 API,创建 OpenSpec 提案
4. 在游戏中验证性能影响

### 更新渲染着色器

1. 修改 Java 端的 Shader 资源文件
2. 更新 Compute Shader 的数据布局
3. 确保 Rust 端输出的数据格式匹配
4. 创建 OpenSpec 提案（渲染系统能力）

### 性能优化工作流

1. 使用 Criterion/JMH 识别瓶颈
2. 优化热路径代码
3. 测量优化效果
4. 如果改变行为,创建 OpenSpec 提案

## 测试策略

**Rust 测试**:
- 单元测试: `cargo test`
- 集成测试: `tests/` 目录
- 性能测试: `cargo bench`（Criterion）

**Java 测试**:
- 单元测试: JUnit
- Mod 集成测试: Fabric/Forge 测试环境

**跨语言集成测试**:
- 在实际 Minecraft 客户端中测试
- 使用测试模组加载测试模型
- 验证 JNI 边界的正确性

## Minecraft 集成

**Fabric 特定**:
- 使用 Fabric API 的渲染事件
- 遵循 Fabric 资源加载约定
- 使用 Fabric 的 Mixin 进行必要的事件注入

**Forge 特定**:
- 使用 Forge 的渲染注册系统
- 遵循 Forge 的事件总线模式
- 使用 Forge 的模型加载器

**跨平台（Architectury）**:
- 通用代码在 `common/` 模块
- 平台特定代码在 `fabric/` 和 `forge/`
- 使用 Architectury 提供的抽象层

## 常见问题

**Q: JNI 调用导致性能下降**
- 检查是否在热循环中频繁跨边界
- 批量处理数据,减少调用次数
- 考虑使用直接内存缓冲区

**Q: 物理模拟不稳定**
- 调整 Rapier 的时间步长
- 检查刚体质量和碰撞形状
- 增加约束迭代次数

**Q: 模型加载失败**
- 验证 PMX 文件格式版本
- 检查纹理路径是否正确
- 确认编码（UTF-8 vs Shift-JIS）

**Q: 着色器编译错误**
- 检查 GLSL 版本兼容性
- 验证数据布局（UBO/SSBO）
- 测试不同 GPU 的兼容性
