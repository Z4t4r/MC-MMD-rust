# Change: Initialize OpenSpec system with English language requirement

## Why

The OpenSpec system has been partially initialized but lacks critical configuration:
1. The `project.md` file contains only placeholder templates without actual project information
2. There is no explicit language requirement for OpenSpec documentation
3. Missing proper project context and conventions for AI assistants

This creates ambiguity when creating proposals and maintaining documentation standards across the project.

## What Changes

- **Update `project.md`** with actual MC-MMD-rust project information, tech stack, and conventions
- **Add English language requirement** to OpenSpec documentation standards
- **Update AGENTS.md** to explicitly state that all OpenSpec documentation must be in English
- **Create initial documentation capability spec** to govern OpenSpec language standards

## UI Design Changes

N/A - This change does not involve user interface modifications.

## Code Flow Changes

N/A - This change is documentation-only and does not affect code execution flow.

## Impact

- **Affected specs**: `documentation` (new capability for OpenSpec documentation standards)
- **Affected code**: None (documentation-only change)
- **Affected documentation**:
  - `openspec/project.md` - Populate with actual project information
  - `openspec/AGENTS.md` - Add explicit English language requirement

## Benefits

- **Clarity**: Establishes clear language expectations for all OpenSpec documentation
- **Consistency**: Ensures international collaborators can work with the codebase effectively
- **Maintainability**: Reduces confusion about documentation language across mixed-language teams
- **Standards**: Provides a foundation for proper spec-driven development workflow
