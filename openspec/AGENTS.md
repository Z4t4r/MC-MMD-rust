# OpenSpec Conventions for MC-MMD-rust

This document defines the OpenSpec workflow conventions specific to the MC-MMD-rust project.

## Overview

MC-MMD-rust uses OpenSpec to manage significant project changes. This system provides:
- Structured proposals for architectural decisions
- Traceable history of project evolution
- Context for AI-assisted development
- Coordination between Rust and Java components

## When to Create a Proposal

Create a proposal for:
- **Feature**: New functionality (animation types, physics effects, rendering features)
- **Refactor**: Code restructuring affecting multiple files or modules (especially JNI interface)
- **Performance**: Significant optimizations requiring architectural changes
- **Architecture**: Changes to Rust/Java boundaries or system design
- **Bugfix**: Major fixes that require understanding system-wide impact

Skip proposals for:
- Trivial bug fixes (typos, simple logic errors)
- Documentation updates
- Test additions
- Configuration tweaks

## Proposal Structure

### Required Files

Each proposal in `openspec/changes/<change-id>/` must include:

1. **proposal.md**: Main proposal document
   - Overview: Brief summary of the change
   - Context: Background information
   - Problem: What problem are we solving?
   - Solution: Proposed approach
   - Scope: In-scope and out-of-scope items
   - Impact: Benefits, affected components, risks
   - Success Criteria: How we know it's complete

2. **tasks.md**: Implementation task list
   - Ordered, actionable tasks
   - Each task has validation criteria
   - Mark dependencies between tasks
   - Group related tasks in phases

3. **specs/<capability>/spec.md**: Capability specification changes (if applicable)
   - ADDED/MODIFIED/REMOVED Requirements
   - Each requirement has scenarios
   - Cross-reference related capabilities

### Optional Files

- **design.md**: Detailed design documentation for complex changes
  - Architecture diagrams
  - Trade-off analysis
  - Alternative approaches considered
  - Implementation details

## Change-ID Naming

Use descriptive, verb-led change IDs:

```
<verb>-<component>-<description>
```

Examples:
- `add-vmd-motion4-support` - Adding new animation format
- `refactor-jni-memory-safety` - Improving JNI interface
- `optimize-gpu-skinning-shader` - Performance optimization
- `fix-uv-morph-rendering` - Major rendering bugfix

## Proposal Types

### Feature
New functionality for end users or developers.
- Example: Add support for VPDMorph format
- Include: User-facing changes, API additions, config options

### Refactor
Internal code improvements without changing functionality.
- Example: Restructure shader resource management
- Include: Code organization improvements, API cleanup
- Must ensure: No behavioral changes, comprehensive testing

### Performance
Optimizations for speed, memory, or resource usage.
- Example: Implement shader attribute caching
- Include: Benchmarks, before/after metrics

### Architecture
Structural changes to system design.
- Example: Separate physics engine into independent crate
- Include: Rationale, migration path, compatibility notes

### Bugfix
Significant defect fixes requiring careful analysis.
- Example: Fix outline alpha blending with shaders
- Include: Root cause analysis, regression prevention

## Proposal Lifecycle

```
Draft → Review → Approved → Implemented → Completed
```

### States

- **Draft**: Initial writing, not ready for review
- **Review**: Ready for maintainer review, feedback welcome
- **Approved**: Approved for implementation
- **Implemented**: Implementation in progress
- **Completed**: All tasks done, validated, and merged

### Transitions

- **Draft → Review**: Author marks ready, all required sections complete
  - Validation: All sections in proposal.md filled out
  - Validation: tasks.md has ordered, actionable items
  - Validation: Spec deltas follow format (if applicable)
  - Action: Update status in proposal.md header

- **Review → Approved**: Maintainer approves, may request changes
  - Validation: Maintainer review complete
  - Validation: Feedback addressed (if any)
  - Action: Maintainer adds "Approved" with name and date

- **Approved → Implemented**: First commit pushed for this proposal
  - Validation: Implementation branch created
  - Validation: First commit references proposal
  - Action: Update status to "Implemented"

- **Implemented → Completed**: All tasks in tasks.md complete
  - Validation: All tasks marked with [x]
  - Validation: All success criteria met
  - Validation: Code merged to main branch
  - Action: Update status with completion date

### State Transition Criteria Details

#### Draft State

- Proposal is being written
- Not yet ready for review
- Author may create drafts in any order
- No validation required yet

#### Review State

- All required files exist (proposal.md, tasks.md)
- proposal.md has all sections filled:
  - Overview, Context, Problem, Solution
  - Scope (In/Out)
  - Impact Assessment
  - Success Criteria
- tasks.md has ordered, actionable tasks
- Each task has validation criteria
- Spec deltas use correct format (ADDED/MODIFIED/REMOVED)
- change-id is unique and follows naming conventions
- Author updates status to "Review"

#### Approved State

- Maintainer has reviewed the proposal
- All reviewer feedback addressed (or documented as deferred)
- Maintainer adds approval metadata:
  ```markdown
  **Approved**: @maintainer-name
  **Approval Date**: YYYY-MM-DD
  **Notes**: [Any conditions or concerns]
  ```
- Proposal is ready for implementation

#### Implemented State

- Implementation has begun
- First commit referencing the proposal has been pushed
- Implementation branch follows naming: `<change-id>/implementation`
- Commit messages reference proposal: `Refs: openspec/<change-id>`
- Author updates status to "Implemented"

#### Completed State

- All tasks in tasks.md are marked [x]
- All success criteria are met
- Implementation is merged to main branch
- PR (if used) is merged and linked to proposal
- Author updates status with completion date
- Proposal is archived as historical record

## Proposal Metadata

At the top of proposal.md, include:

```markdown
# Proposal Title

**Status**: Review
**Type**: Feature
**Author**: @username
**Created**: 2025-01-XX
**Approved**: @maintainer (when approved)
**Completed**: 2025-02-XX (when complete)
```

## Bilingual Documentation

This project supports both Chinese and English documentation:

- **Technical specs**: Prefer English for precision
- **User-facing features**: Use Chinese matching project style
- **Code comments**: Match existing codebase language
- **Proposal rationale**: Use author's preference, consider translation for clarity

## Spec Delta Format

Spec deltas track changes to system capabilities. They use Given-When-Then format to define requirements and scenarios.

### Spec File Structure

Spec files are located at: `openspec/changes/<change-id>/specs/<capability>/spec.md`

Example structure:
```
openspec/changes/add-vpdmorph-support/
├── proposal.md
├── tasks.md
└── specs/
    └── morph-system/
        └── spec.md
```

### Requirements

Use `## ADDED|MODIFIED|REMOVED Requirements` headers:

```markdown
## ADDED Requirements

### Requirement: Feature description

Clear description of what the system must do.

#### Scenario: Specific situation

**Given** preconditions
**When** action occurs
**Then** expected outcome
**And** additional assertions
```

### Requirement Format Examples

#### Example 1: New Feature (ADDED)

```markdown
## ADDED Requirements

### Requirement: VPDMorph Format Support

The system MUST support loading and rendering VPDMorph animation files
for vertex-based morph animations in MMD models.

#### Scenario: Loading a VPDMorph file

**Given** a VPDMorph file exists at `.minecraft/3d-skin/CustomMorph/test.vpd`
**When** the model loading system reads the file
**Then** the morph data is parsed successfully
**And** vertex offsets are stored in the morph manager
**And** the morph appears in the model configuration UI

#### Scenario: Applying a VPDMorph during animation

**Given** a model has VPDMorph data loaded
**When** the animation timeline reaches the morph's keyframe
**Then** the vertex positions are modified by the morph offsets
**And** the modified vertices are rendered in the OpenGL pipeline
**And** frame rate remains above 30 FPS
```

#### Example 2: Modifying Existing Behavior (MODIFIED)

```markdown
## MODIFIED Requirements

### Requirement: JNI Memory Safety

The JNI interface MUST use proper memory management to prevent memory leaks
 and use-after-free errors when passing data between Rust and Java.

#### Scenario: Transferring ownership of model data

**Given** a PMX model is loaded in Rust
**When** the model data is transferred to Java via JNI
**Then** ownership is transferred to Java (Rust releases, Java holds)
**And** the Java finalizer properly frees native memory
**And** no double-free errors occur during gameplay

#### Scenario: Borrowing model data for rendering

**Given** a model exists in Rust memory
**When** Java requests read-only access for rendering
**Then** a borrowed pointer is provided without transferring ownership
**And** Rust's lifetime guarantees prevent use-after-free
**And** Java cannot modify the borrowed data
```

#### Example 3: Removing Deprecated Features (REMOVED)

```markdown
## REMOVED Requirements

### Requirement: Legacy Physics Format

Previously: The system supported loading physics data from legacy .phy format.

**Reason**: Replaced by PMX-embedded physics for better consistency

**Migration**: Existing .phy files are ignored; users should use models with
embedded PMX physics or re-export from PMX Editor

#### Scenario: Attempting to load legacy .phy file

**Given** a legacy .phy file exists in the resource directory
**When** the physics system initializes
**Then** the .phy file is ignored with a warning log
**And** the model uses default physics instead
**And** gameplay continues without errors
```

### Cross-References

Reference related capabilities:

```markdown
## Related Capabilities

- **Physics**: Physics engine integration
- **Rendering**: OpenGL pipeline
```

### Spec Delta Validation

When creating spec deltas:

1. **Each ADDED requirement** must have at least one scenario
2. **Scenarios** must use Given-When-Then format
3. **MODIFIED requirements** should highlight what changed and why
4. **REMOVED requirements** should document migration path
5. **Cross-references** link to related capabilities for context

## Validation Checklist

Before marking a proposal as "Review":

- [ ] proposal.md has all required sections
- [ ] tasks.md has ordered, actionable tasks
- [ ] Each task has validation criteria
- [ ] Spec deltas follow format (if applicable)
- [ ] change-id is unique and descriptive
- [ ] Impact assessment includes compatibility notes
- [ ] Success criteria are measurable

## Integration with Git Workflow

### Branch Naming

When implementing a proposal:

```
<change-id>/implementation
```

Example: `refactor-jni-interface/implementation`

### Commit Messages

Reference proposal in commits:

```
<commit message>

Refs: openspec/<change-id>
```

### Pull Requests

Link PR to proposal:

```
# PR Title

Implements the proposal for X feature.

Relates to: openspec/changes/<change-id>
```

## Template Locations

Templates are available in `openspec/templates/`:
- `openspec/templates/proposal.md` - Main proposal document template
- `openspec/templates/tasks.md` - Implementation task list template
- `openspec/templates/design.md` - Optional design document template (for complex changes)

### Using Templates

```bash
# Copy templates when creating a new proposal
cp openspec/templates/proposal.md openspec/changes/<change-id>/proposal.md
cp openspec/templates/tasks.md openspec/changes/<change-id>/tasks.md
# design.md is optional, only copy if needed for complex changes
cp openspec/templates/design.md openspec/changes/<change-id>/design.md
```

### Example Proposals

For reference examples:
- See `openspec/changes/openspec-change-proposal-system/` as reference
- Future proposals will serve as additional examples

## Tools and Commands

### Creating a New Proposal

```bash
# Create proposal directory
mkdir -p openspec/changes/<change-id>/specs

# Create proposal.md from template
# (copy structure from existing proposals)

# Create tasks.md from template
# (copy structure from existing proposals)

# Edit and validate
```

### Manual Validation

Since OpenSpec tooling may not be installed, manually validate:

1. Check proposal.md sections are complete
2. Verify tasks.md has ordered, actionable items
3. Confirm spec deltas use correct format
4. Ensure all files exist in proposal directory

### Listing Proposals

```bash
# List all proposals
ls openspec/changes/

# Show proposal details
cat openspec/changes/<change-id>/proposal.md

# Show implementation tasks
cat openspec/changes/<change-id>/tasks.md
```

## Maintainer Guidelines

When reviewing proposals:

1. **Clarity**: Is the problem and solution clear?
2. **Completeness**: Are all sections filled out adequately?
3. **Feasibility**: Can this be implemented realistically?
4. **Impact**: Are benefits and risks properly assessed?
5. **Alternatives**: Were alternative approaches considered?

When approving proposals:

1. Add **Approved** status with your name
2. Add approval timestamp
3. Note any conditions or concerns
4. Communicate approval to team

## AI Assistant Usage

When working with AI assistants (like Claude Code):

1. Reference proposals for context: "Implement openspec/changes/<change-id>"
2. Ask AI to create proposals for major changes
3. Use proposals to maintain conversation context across sessions
4. Let AI validate proposals against this checklist

## Questions?

For questions about OpenSpec usage in this project:
1. Check this document (AGENTS.md)
2. Review existing proposals for examples
3. Consult project maintainers
4. Refer to openspec/changes/openspec-change-proposal-system/ for system details
