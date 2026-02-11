# OpenSpec Proposal Authoring Guide

This guide helps you create effective change proposals for the MC-MMD-rust project.

## When to Create a Proposal

Create a proposal for significant changes:
- **Feature**: New functionality (animation types, physics effects, rendering features)
- **Refactor**: Code restructuring affecting multiple files or modules (especially JNI interface)
- **Performance**: Significant optimizations requiring architectural changes
- **Architecture**: Changes to Rust/Java boundaries or system design
- **Bugfix**: Major fixes that require understanding system-wide impact

**Skip proposals for**:
- Trivial bug fixes (typos, simple logic errors)
- Documentation updates
- Test additions
- Configuration tweaks

## Quick Start

### Step 1: Create Proposal Directory

```bash
# Navigate to project root
cd /path/to/MC-MMD-rust

# Create proposal directory with descriptive change-id
mkdir -p openspec/changes/<change-id>/specs
```

### Step 2: Copy Templates

```bash
# Copy required templates
cp openspec/templates/proposal.md openspec/changes/<change-id>/proposal.md
cp openspec/templates/tasks.md openspec/changes/<change-id>/tasks.md

# Optional: Copy design template for complex changes
cp openspec/templates/design.md openspec/changes/<change-id>/design.md
```

### Step 3: Fill Out proposal.md

Edit `proposal.md` with your proposal details:
1. Update header metadata (Status, Type, Author, Created)
2. Fill in Overview (2-3 sentence summary)
3. Add Context (background information)
4. Describe Problem (what you're solving)
5. Propose Solution (your approach)
6. Define Scope (what's in/out)
7. Assess Impact (benefits, risks, compatibility)
8. List Success Criteria (measurable outcomes)

### Step 4: Create tasks.md

Edit `tasks.md` with implementation plan:
1. Break down work into phases
2. Create ordered, actionable tasks
3. Add validation criteria for each task
4. Document dependencies between tasks

### Step 5: Create Spec Deltas (if applicable)

```bash
# Create spec file for capability changes
mkdir -p openspec/changes/<change-id>/specs/<capability>/
touch openspec/changes/<change-id>/specs/<capability>/spec.md
```

Edit `spec.md` with:
- ADDED/MODIFIED/REMOVED Requirements
- Scenarios using Given-When-Then format
- Related capabilities cross-references

### Step 6: Validate and Submit

```bash
# Run validation checklist (see below)
# Update status to "Review" when ready
git add openspec/changes/<change-id>/
git commit -m "Draft proposal: <title>"
```

## Proposal Structure Reference

### proposal.md Sections

| Section | Purpose | Tips |
|---------|---------|------|
| **Overview** | Quick summary | 2-3 sentences, what and why |
| **Context** | Background | Link to relevant docs, explain current state |
| **Problem** | What's broken | Quantify impact, explain why it matters |
| **Solution** | Your approach | Technical details, why this solution |
| **Scope** | Boundaries | Be clear about what's NOT included |
| **Impact** | Effects | Benefits, risks, compatibility |
| **Success Criteria** | Measurable goals | Specific, testable outcomes |

### tasks.md Format

```markdown
## Phase 1: [Name]

### 1.1 [Task Title]

- [ ] Subtask 1
- [ ] Subtask 2

**Description**: What this task involves

**Files**: list/files/to/modify.md

**Validation**: How to verify completion

**Dependencies**: Task X.Y (reason)
```

### Spec Delta Format

```markdown
## ADDED Requirements

### Requirement: Feature description

What the system must do.

#### Scenario: Situation name

**Given** precondition
**When** action
**Then** outcome
**And** additional assertions
```

## Proposal Types Guide

### Feature Proposals

**Use for**: New user-facing or developer-facing functionality

**Include**:
- User benefit / use case
- API changes (if any)
- Configuration options
- Migration from old behavior (if replacing something)

**Example**: Add VPDMorph format support

### Refactor Proposals

**Use for**: Internal code improvements without behavior changes

**Include**:
- Current problems (maintainability, technical debt)
- Proposed new structure
- Why this won't break existing behavior
- Testing strategy to ensure no behavioral changes

**Example**: Restructure shader resource management

### Performance Proposals

**Use for**: Optimizations improving speed, memory, or resource usage

**Include**:
- Current performance metrics (baseline)
- Target performance metrics
- Profiling data showing bottleneck
- Why optimization requires architectural change
- Benchmark methodology

**Example**: Implement shader attribute caching

### Architecture Proposals

**Use for**: Structural changes to system design

**Include**:
- Current architecture diagram
- Proposed architecture diagram
- Rationale for change
- Migration strategy
- Compatibility notes
- Rollback plan

**Example**: Separate physics engine into independent crate

### Bugfix Proposals

**Use for**: Significant defects requiring careful analysis

**Include**:
- Root cause analysis
- Why this fix is safe
- Regression testing plan
- How to prevent similar bugs

**Example**: Fix outline alpha blending with shaders

## Writing Effective Proposals

### Do's

- **Be specific**: Use concrete examples and numbers
- **Show research**: Demonstrate you understand the current system
- **Consider alternatives**: Explain why you chose this approach
- **Think about users**: How does this benefit players or developers?
- **Plan for testing**: How will you verify this works?

### Don'ts

- **Don't be vague**: Avoid "improve performance" without metrics
- **Don't skip risks**: Acknowledge potential downsides
- **Don't ignore migration**: How does this affect existing code/users?
- **Don't forget edge cases**: What happens with invalid input, errors, etc.?

## Change-ID Naming Convention

Use descriptive, verb-led names:

```
<verb>-<component>-<description>
```

**Examples**:
- `add-vpdmorph-support` - New VPDMorph format feature
- `refactor-jni-memory-safety` - JNI interface improvements
- `optimize-shader-caching` - Performance optimization
- `fix-uv-morph-rendering` - Rendering bugfix
- `architecture-separate-physics-crate` - System redesign

**Tips**:
- Use lowercase with hyphens
- Be specific enough to distinguish from other proposals
- Start with action verb (add, refactor, optimize, fix)
- Include component name if change is localized

## Validation Checklist

Before marking your proposal as "Review", verify:

### proposal.md
- [ ] All required sections filled out
- [ ] Overview is concise (2-3 sentences)
- [ ] Problem statement is clear
- [ ] Solution approach is described
- [ ] Scope explicitly defines in/out items
- [ ] Impact assessment includes benefits and risks
- [ ] Success criteria are measurable
- [ ] Alternatives considered (if applicable)

### tasks.md
- [ ] Tasks are ordered logically
- [ ] Each task is actionable
- [ ] Each task has validation criteria
- [ ] Dependencies are documented
- [ ] Tasks are grouped in phases
- [ ] No vague tasks like "implement feature"

### spec.md (if applicable)
- [ ] Uses ADDED/MODIFIED/REMOVED headers
- [ ] Each requirement has at least one scenario
- [ ] Scenarios use Given-When-Then format
- [ ] Related capabilities are cross-referenced

### General
- [ ] change-id is unique and follows naming convention
- [ ] All required files exist in proposal directory
- [ ] Proposal type is appropriate for the change
- [ ] Language is clear and understandable

## Common Pitfalls

### 1. Vague Success Criteria

**Bad**: "Performance is better"
**Good**: "Frame rate improves from 25 FPS to 45+ FPS with complex models"

### 2. Missing Migration Path

**Bad**: "Old format no longer supported"
**Good**: "Old .phy files are ignored with a warning log. Users should use PMX-embedded physics or re-export from PMX Editor"

### 3. Undefined Scope

**Bad**: "Improve rendering" (too broad)
**Good**: "Implement shader attribute caching for GPU skinning pipeline" (specific)

### 4. No Risk Assessment

**Bad**: "This will work fine"
**Good**: "Risk: Increased memory usage for cached attributes. Mitigation: Cache size limit and LRU eviction policy"

### 5. Unordered Tasks

**Bad**: Tasks in random order
**Good**: Tasks ordered by dependencies, from foundation to completion

## Example Proposal

See the proposal system itself as an example:
- `openspec/changes/openspec-change-proposal-system/proposal.md`

## Getting Help

If you're unsure about any part of proposal writing:

1. **Check AGENTS.md**: For detailed conventions and format requirements
2. **Review existing proposals**: Use them as templates
3. **Ask maintainers**: Get feedback before investing too much time
4. **Start with Draft**: Set status to "Draft" and request feedback

## Proposal Lifecycle

```
Draft (you're writing)
  ↓ (when complete and validated)
Review (ready for maintainer feedback)
  ↓ (when approved)
Approved (ready to implement)
  ↓ (when implementation starts)
Implemented (implementation in progress)
  ↓ (when all tasks complete)
Completed (done and merged)
```

## Resources

- **Templates**: `openspec/templates/`
- **Conventions**: `openspec/AGENTS.md`
- **Project Context**: `openspec/project.md`
- **Learning Guide**: `docs/high-level-guide/README.md`
