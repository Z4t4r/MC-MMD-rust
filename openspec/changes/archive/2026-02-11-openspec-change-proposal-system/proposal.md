# OpenSpec Change Proposal System

**Status**: Implemented
**Type**: Architecture
**Author**: OpenSpec System
**Created**: 2025-02-11
**Approved**: Self-approved (system proposal)
**Completed**: 2025-02-11 (Phases 1-4 complete; Phase 5 requires team adoption)

## Overview

Establish a structured change proposal management system for the MC-MMD-rust project to track major feature changes, architectural adjustments, and provide structured context for AI-assisted development.

## Context

**MC-MMD-rust** is a Minecraft Mod for version 1.21.1 (not 1.20.1 as stated in outdated README) that implements MikuMikuDance model rendering and physics simulation.

### Project Architecture

- **rust_engine**: Rust-based MMD physics and animation engine
  - PMX/VMD format parsing
  - Skeletal hierarchy management
  - Physics simulation (Rapier3D)
  - JNI bindings for Java layer interaction
- **Minecraft Mod**: Java-based rendering and integration layer (supports Fabric/NeoForge)
  - OpenGL model rendering
  - Compute Shader skinning
  - Iris shader compatibility

### Technology Stack
- Rust 1.70+ (physics engine)
- Java 21+ (Minecraft mod)
- Gradle 8.x (build system)
- Rapier3D (3D physics engine)
- OpenGL (rendering)

## Problem

The current project lacks a standardized change proposal management system, which creates several challenges:

1. **No structured change tracking**: Major architectural decisions and feature additions are not documented in a systematic way
2. **Fragmented development history**: Without formal proposals, the rationale behind significant changes becomes unclear over time
3. **AI context limitations**: AI assistants lack structured historical context about the project's evolution
4. **Multi-language coordination**: Complex Rust + Java integration requires careful coordination without formal documentation
5. **Knowledge silos**: Contributors may struggle to understand the reasoning behind past architectural decisions

## Solution

Establish an OpenSpec-compliant change proposal system with the following components:

### 1. Standardized Proposal Templates

- Define unified proposal format and structure
- Include core sections: context, problem statement, solution, impact assessment
- Support bilingual documentation (Chinese/English) matching existing project documentation style

### 2. Proposal Type Classification

- **Feature**: New functionality (e.g., new animation types, physics effects)
- **Refactor**: Code restructuring (e.g., JNI interface optimization)
- **Performance**: Performance optimizations (e.g., GPU skinning improvements)
- **Bugfix**: Major defect fixes
- **Architecture**: Architectural adjustments (e.g., Rust/Java boundary refactoring)

### 3. Version Control Integration

- Store proposal files in `openspec/changes/<change-id>/` directory
- Use semantic version naming conventions
- Associate with Git commits and branch information

### 4. Approval Workflow

- States: Draft → Review → Approved → Implemented → Completed
- Record approvers and timestamps
- Support proposal status tracking

## Scope

### In Scope

- Creating `openspec/` directory structure and conventions
- Defining proposal templates for all change types
- Establishing proposal lifecycle workflow
- Creating documentation for proposal authors
- Setting up validation and tooling support

### Out of Scope

- Modifying existing rust_engine or Minecraft Mod code
- Implementing specific feature changes (those will be separate proposals)
- Changing existing Git workflow (complementary, not replacement)
- Automated proposal enforcement (manual initially, automated later if needed)

## Impact Assessment

### Expected Benefits

1. **Structured change history**: Clear audit trail of project evolution
2. **Improved collaboration**: Better cross-language (Rust + Java) development coordination
3. **AI assistant context**: Structured historical context for AI-assisted development
4. **Reduced knowledge fragmentation**: Architectural decisions documented and retrievable
5. **Onboarding support**: New contributors can understand project evolution through proposals

### Affected Components

- **New**: `openspec/` directory structure creation
- **Modified**: Project documentation system (additions only)
- **Modified**: Development workflow (new process, not code changes)

### Compatibility

- **No impact** on existing rust_engine and Minecraft Mod code
- **Compatible** with existing Git workflow
- **Backwards compatible** with released v1.0.2 version history追溯

### Risks

- **Adoption risk**: Developers may not follow proposal process consistently
  - *Mitigation*: Keep process lightweight, provide templates, integrate with existing workflow
- **Maintenance overhead**: Proposals may become outdated
  - *Mitigation*: Clear lifecycle states, periodic review process
- **Bureaucracy risk**: Excessive process could slow development
  - *Mitigation*: Use for major changes only, minor changes can bypass formal proposal

## Success Criteria

1. **Documentation exists**: Clear proposal templates and guidelines created
2. **Validatable**: Proposals can be validated against OpenSpec standards
3. **Usable**: Developers can create proposals without excessive overhead
4. **Discoverable**: Existing and new proposals can be easily listed and searched
5. **Compatible**: Works with existing project structure and Git workflow

## Alternatives Considered

### Alternative 1: Use GitHub Issues/PRs only
- **Pros**: Familiar workflow, built-in tooling
- **Cons**: Not structured for architectural decisions, no spec tracking
- **Decision**: Complement with OpenSpec for major changes, continue using Issues/PRs for implementation

### Alternative 2: ADR (Architecture Decision Records)
- **Pros**: Industry standard for architectural decisions
- **Cons**: Focused on decisions, not full change lifecycle, no spec integration
- **Decision**: OpenSpec provides richer structure with spec deltas and task tracking

### Alternative 3: No formal system (status quo)
- **Pros**: Zero overhead
- **Cons**: Lost context, poor coordination, difficult for AI assistance
- **Decision**: Project complexity justifies formal system for major changes

## Timeline

- **Phase 1**: Create proposal structure and templates (this proposal)
- **Phase 2**: Create project.md and AGENTS.md if not exists
- **Phase 3**: Validate and iterate on proposal system
- **Phase 4**: Use system for next major change

## References

- OpenSpec methodology documentation
- Project README.md for architecture overview
- Existing v1.0.2 release history
