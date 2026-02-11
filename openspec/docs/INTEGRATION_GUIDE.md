# OpenSpec Git Workflow Integration Guide

This guide explains how to integrate OpenSpec proposals with your Git workflow.

## Overview

OpenSpec proposals complement the existing Git workflow:
- **Proposals** document the what and why
- **Git branches/commits** implement the how
- **Pull requests** review the implementation

## Workflow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Create Proposal                                           │
│    - Draft proposal.md and tasks.md                         │
│    - Status: Draft                                          │
│    - No Git activity yet                                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Submit for Review                                        │
│    - Status: Review                                         │
│    - Commit: "Draft proposal: <title>"                      │
│    - Push to main or feature branch                         │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Approval                                                 │
│    - Maintainer approves                                    │
│    - Status: Approved                                       │
│    - Commit: Update proposal.md with approval metadata      │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Implementation                                          │
│    - Create implementation branch                           │
│    - Status: Implemented                                    │
│    - Work through tasks.md                                  │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Pull Request                                             │
│    - Link PR to proposal                                    │
│    - Review implementation                                  │
│    - Merge to main                                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│ 6. Completion                                               │
│    - All tasks complete                                     │
│    - Status: Completed                                      │
│    - Commit: Update proposal.md with completion date        │
└─────────────────────────────────────────────────────────────┘
```

## Branch Naming Conventions

### Proposal Branch

For the proposal itself (optional):

```
proposal/<change-id>
```

Example: `proposal/add-vpdmorph-support`

Use this for:
- Drafting the proposal
- Getting feedback on proposal structure
- Proposal revisions before approval

### Implementation Branch

Once approved, create implementation branch:

```
<change-id>/implementation
```

Example: `add-vpdmorph-support/implementation`

## Commit Message Conventions

### Proposal Commits

When creating or updating the proposal:

```bash
# Initial proposal draft
git add openspec/changes/<change-id>/
git commit -m "Draft proposal: <title>"

# Proposal revisions
git add openspec/changes/<change-id>/proposal.md
git commit -m "Revise proposal: <change-id> - address feedback"

# Move to review state
git add openspec/changes/<change-id>/proposal.md
git commit -m "Submit proposal for review: <change-id>"
```

### Implementation Commits

When implementing the proposal:

```bash
# Reference the proposal in implementation commits
git commit -m "<implementation description>

Refs: openspec/<change-id>"
```

Example:
```bash
git commit -m "Add VPDMorph file parser

- Implement .vpd format parsing
- Add vertex offset storage
- Integrate with morph manager

Refs: openspec/add-vpdmorph-support"
```

### Proposal State Updates

When updating proposal status:

```bash
# Move to Implemented
git commit -m "Start implementation: <change-id>

Update proposal status to Implemented"

# Mark completed
git commit -m "Complete proposal: <change-id>

All tasks complete, proposal marked as Completed"
```

## Pull Request Integration

### PR Description Template

```markdown
# [PR Title]

Implements the proposal for [brief description].

## Related Proposal

- **Proposal**: openspec/changes/<change-id>
- **Type**: [Feature/Refactor/Performance/Architecture/Bugfix]
- **Proposal Status**: [Implemented/Completed]

## Implementation Summary

[What was implemented]

## Changes Made

- [ ] Task 1.1: [Description]
- [ ] Task 1.2: [Description]
- [ ] Task 2.1: [Description]
- ...

## Testing

[How this was tested]

## Checklist

- [ ] Code follows project style guidelines
- [ ] All tasks in proposal tasks.md are complete
- [ ] Tests pass locally
- [ ] Documentation updated (if applicable)
- [ ] No unintended breaking changes

## Notes

[Any additional notes for reviewers]
```

### Linking PR to Proposal

In the PR body, always include:

```markdown
Relates to: openspec/changes/<change-id>
```

Consider adding to PR title if space allows:

```
[add-vpdmorph-support] Add VPDMorph file parser and renderer
```

## Complete Workflow Example

### Step 1: Create and Draft Proposal

```bash
# Create proposal directory
mkdir -p openspec/changes/add-vpdmorph-support/specs

# Copy templates
cp openspec/templates/proposal.md openspec/changes/add-vpdmorph-support/proposal.md
cp openspec/templates/tasks.md openspec/changes/add-vpdmorph-support/tasks.md

# Edit proposal files
vim openspec/changes/add-vpdmorph-support/proposal.md
vim openspec/changes/add-vpdmorph-support/tasks.md

# Commit proposal draft
git add openspec/changes/add-vpdmorph-support/
git commit -m "Draft proposal: Add VPDMorph format support"
git push
```

### Step 2: Submit for Review

```bash
# Update status to Review in proposal.md
vim openspec/changes/add-vpdmorph-support/proposal.md

# Submit
git add openspec/changes/add-vpdmorph-support/proposal.md
git commit -m "Submit proposal for review: add-vpdmorph-support"
git push
```

### Step 3: Get Approved

Maintainer reviews and approves. Update proposal.md with:

```markdown
**Approved**: @maintainer
**Approval Date**: 2025-01-20
```

Commit the approval:

```bash
git add openspec/changes/add-vpdmorph-support/proposal.md
git commit -m "Approve proposal: add-vpdmorph-support"
git push
```

### Step 4: Create Implementation Branch

```bash
# Create implementation branch
git checkout -b add-vpdmorph-support/implementation

# Update proposal status to Implemented
vim openspec/changes/add-vpdmorph-support/proposal.md
git add openspec/changes/add-vpdmorph-support/proposal.md
git commit -m "Start implementation: add-vpdmorph-support"
```

### Step 5: Implement

```bash
# Work through tasks in tasks.md
# Each task gets one or more commits

git commit -m "Add VPDMorph file format parser

- Implement .vpd binary format reading
- Parse vertex offsets and morph names
- Handle format version differences

Refs: openspec/add-vpdmorph-support"

git commit -m "Integrate VPDMorph with morph system

- Add VPDMorph data to morph manager
- Update UI to show VPDMorph files
- Implement morph blending with vertex offsets

Refs: openspec/add-vpdmorph-support"
```

### Step 6: Create Pull Request

```bash
# Push implementation branch
git push -u origin add-vpdmorph-support/implementation

# Create PR via GitHub/GitLab UI
# Use PR description template from above
```

### Step 7: Complete Proposal

After PR is merged:

```bash
# Checkout main
git checkout main
git pull

# Update proposal status to Completed
vim openspec/changes/add-vpdmorph-support/proposal.md

# Mark all tasks as [x] in tasks.md
vim openspec/changes/add-vpdmorph-support/tasks.md

git add openspec/changes/add-vpdmorph-support/
git commit -m "Complete proposal: add-vpdmorph-support

All tasks complete, PR merged"
git push
```

## Proposal-to-Commit Mapping

### Tracking Progress

Keep proposal tasks.md in sync with implementation:

```markdown
## Phase 1: Core Implementation

### 1.1 Implement VPDMorph parser

- [x] Parse .vpd binary format (commit: abc123)
- [x] Extract vertex offsets (commit: def456)
- [x] Handle morph names (commit: ghi789)

**Status**: Completed in commits abc123, def456, ghi789
```

### Referencing Tasks in Commits

Optionally reference specific tasks:

```bash
git commit -m "Implement VPDMorph vertex offset parsing

Completes task 1.1.2 from proposal tasks.md

Refs: openspec/add-vpdmorph-support"
```

## Multiple Proposals in One Branch

Sometimes multiple proposals are implemented together:

### Sequential Proposals

If proposal B depends on proposal A:

1. Implement and merge proposal A first
2. Then implement proposal B
3. Reference both in commits: `Refs: openspec/proposal-a, openspec/proposal-b`

### Related Proposals

If proposals are related but independent:

1. Create separate implementation branches
2. Each branch references only its own proposal
3. Merge in order if there are dependencies

## Workflow Diagram with Git Commands

```
┌────────────────────────────────────────────────────────────┐
│ DRAFT STATE                                                 │
├────────────────────────────────────────────────────────────┤
│ mkdir -p openspec/changes/<id>/specs                        │
│ cp openspec/templates/*.md openspec/changes/<id>/           │
│ # Edit files                                                │
│ git add openspec/changes/<id>/                              │
│ git commit -m "Draft proposal: <title>"                     │
└────────────────────┬───────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────┐
│ REVIEW STATE                                                │
├────────────────────────────────────────────────────────────┤
│ # Update status in proposal.md                             │
│ git commit -m "Submit for review: <id>"                    │
│ # Get feedback, revise as needed                           │
└────────────────────┬───────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────┐
│ APPROVED STATE                                              │
├────────────────────────────────────────────────────────────┤
│ # Maintainer adds approval to proposal.md                  │
│ git commit -m "Approve proposal: <id>"                     │
└────────────────────┬───────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────┐
│ IMPLEMENTED STATE                                           │
├────────────────────────────────────────────────────────────┤
│ git checkout -b <id>/implementation                        │
│ # Update status in proposal.md                             │
│ git commit -m "Start implementation: <id>"                 │
│ # Implement tasks with proposal references                 │
│ git commit -m "Implement feature\n\nRefs: openspec/<id>"   │
└────────────────────┬───────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────┐
│ PULL REQUEST                                                 │
├────────────────────────────────────────────────────────────┤
│ git push -u origin <id>/implementation                     │
│ # Create PR with proposal link                             │
│ # PR description: "Relates to: openspec/changes/<id>"     │
└────────────────────┬───────────────────────────────────────┘
                     │
                     ▼
┌────────────────────────────────────────────────────────────┐
│ COMPLETED STATE                                             │
├────────────────────────────────────────────────────────────┤
│ git checkout main && git pull                              │
│ # Update status in proposal.md                             │
│ # Mark all tasks [x] in tasks.md                           │
│ git commit -m "Complete proposal: <id>"                    │
└────────────────────────────────────────────────────────────┘
```

## Best Practices

### Do's

- **Reference proposals in all implementation commits**
- **Keep proposal status in sync with actual progress**
- **Link PRs to proposals in description**
- **Mark tasks complete as you finish them**
- **Update proposal if implementation deviates from plan**

### Don'ts

- **Don't implement without proposal approval** (for major changes)
- **Don't forget to reference the proposal** in commits
- **Don't let proposal status get stale** compared to reality
- **Don't merge implementation before proposal is updated**

## Troubleshooting

### Implementation Deviates from Plan

If implementation ends up different from proposal:

1. **Update proposal.md** to reflect actual approach
2. **Update tasks.md** to add/remove tasks as needed
3. **Document the deviation** in proposal.md or a note

```markdown
## Implementation Notes

The actual implementation differs from the original plan:
- Original: Use approach X
- Actual: Used approach Y
- Reason: Approach X didn't work because Z
```

### Proposal Needs Revision During Implementation

If you discover the proposal is incomplete or incorrect:

1. **Pause implementation**
2. **Update proposal** with new information
3. **Get re-approval** if changes are significant
4. **Resume implementation**

### Multiple Contributors on One Proposal

If multiple people are implementing:

1. **Coordinate who does which tasks**
2. **Each person references the proposal in their commits**
3. **Update tasks.md** as tasks are completed
4. **Communicate about task dependencies**

## Resources

- **Authoring Guide**: `openspec/docs/AUTHORING_GUIDE.md`
- **Reviewer Guide**: `openspec/docs/REVIEWER_GUIDE.md`
- **Conventions**: `openspec/AGENTS.md`
- **Templates**: `openspec/templates/`
