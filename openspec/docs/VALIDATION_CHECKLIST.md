# OpenSpec Proposal Validation Checklist

Use this checklist to validate proposals before marking them as "Review".

## Quick Validation

Run these quick checks first (should take 2-3 minutes):

```bash
# Navigate to proposal directory
cd openspec/changes/<change-id>

# Check required files exist
ls proposal.md tasks.md

# Check for spec directory (optional but common)
ls specs/

# Quick syntax check
grep -E "^#" proposal.md | head -5  # Should show markdown headers
```

## proposal.md Validation

### Required Sections

Check each section exists and is not empty:

- [ ] **Header metadata** present:
  - [ ] Status: (Draft/Review/Approved/Implemented/Completed)
  - [ ] Type: (Feature/Refactor/Performance/Architecture/Bugfix)
  - [ ] Author: @username
  - [ ] Created: YYYY-MM-DD

- [ ] **Overview** section exists:
  - [ ] Concise (2-3 sentences)
  - [ ] Explains what and why
  - [ ] No placeholder text

- [ ] **Context** section exists:
  - [ ] Provides background
  - [ ] Explains current state
  - [ ] Links to relevant docs if needed

- [ ] **Problem** section exists:
  - [ ] Clear problem statement
  - [ ] Specific, not vague
  - [ ] Explains impact

- [ ] **Solution** section exists:
  - [ ] Describes proposed approach
  - [ ] Explains technical details
  - [ ] Justifies why this solution

- [ ] **Scope** section exists:
  - [ ] "In Scope" subsection with specific items
  - [ ] "Out of Scope" subsection with boundaries

- [ ] **Impact Assessment** section exists:
  - [ ] Expected benefits listed
  - [ ] Affected components identified
  - [ ] Compatibility addressed
  - [ ] Risks identified with mitigations

- [ ] **Success Criteria** section exists:
  - [ ] Multiple criteria listed
  - [ ] Each criterion is measurable
  - [ ] Specific outcomes defined

### Optional but Recommended Sections

- [ ] **Alternatives Considered**:
  - [ ] At least one alternative described
  - [ ] Rationale for chosen approach

### proposal.md Format Checks

```bash
# Check for required markdown headers
grep -E "^## (Overview|Context|Problem|Solution|Scope|Impact)" proposal.md

# Should find all 6 headers above
```

## tasks.md Validation

### Required Content

- [ ] **Tasks are ordered**:
  - [ ] Phase 1 before Phase 2, etc.
  - [ ] Logical dependency order
  - [ ] Numbered (1.1, 1.2, 2.1, etc.)

- [ ] **Each task has**:
  - [ ] Descriptive title
  - [ ] Checkbox list for subtasks
  - [ ] Description field
  - [ ] Validation criteria

- [ ] **Task content quality**:
  - [ ] Tasks are actionable (not vague)
  - [ ] Each task has validation criteria
  - [ ] Dependencies documented
  - [ ] Tasks grouped in phases

### tasks.md Format Checks

```bash
# Check for phase headers
grep -E "^## Phase [0-9]+" tasks.md

# Check for task headers
grep -E "^### [0-9]+\.[0-9]+" tasks.md

# Check for validation sections
grep -E "^\*\*Validation\*\*:" tasks.md
```

## spec.md Validation (if present)

### Required Format

- [ ] **Delta markers present**:
  - [ ] `## ADDED Requirements` or
  - [ ] `## MODIFIED Requirements` or
  - [ ] `## REMOVED Requirements`

- [ ] **Requirements have scenarios**:
  - [ ] Each requirement has `#### Scenario: ...`
  - [ ] Scenarios use Given-When-Then format:
    - [ ] `**Given**` preconditions
    - [ ] `**When**` actions
    - [ ] `**Then**` outcomes
    - [ ] `**And**` additional assertions (if needed)

- [ ] **Cross-references** (optional but recommended):
  - [ ] `## Related Capabilities` section
  - [ ] Links to related specs

### spec.md Format Checks

```bash
# Check for requirement markers
grep -E "^## (ADDED|MODIFIED|REMOVED) Requirements" specs/*/spec.md

# Check for scenarios
grep -E "^#### Scenario:" specs/*/spec.md

# Check for Given-When-Then
grep -E "^\*\*(Given|When|Then|And)\*\*" specs/*/spec.md
```

## General Validation

### Change-ID

- [ ] **Descriptive and clear**:
  - [ ] Follows `<verb>-<component>-<description>` format
  - [ ] Lowercase with hyphens
  - [ ] Unique (no other proposal with same ID)

### Proposal Type

- [ ] **Type is appropriate**:
  - [ ] Feature for new functionality
  - [ ] Refactor for code restructuring
  - [ ] Performance for optimizations
  - [ ] Architecture for structural changes
  - [ ] Bugfix for major fixes

### Files and Structure

```bash
# Verify directory structure
ls -la openspec/changes/<change-id>/
# Should show:
# proposal.md
# tasks.md
# specs/ (optional)
```

- [ ] **Directory exists**: `openspec/changes/<change-id>/`
- [ ] **Required files present**: proposal.md, tasks.md
- [ ] **Git tracked**: Files are not in .gitignore

## Validation Script (Manual)

Copy and run this script to validate a proposal:

```bash
#!/bin/bash
# validate-proposal.sh - Manual proposal validation

PROPOSAL_ID=$1

if [ -z "$PROPOSAL_ID" ]; then
  echo "Usage: ./validate-proposal.sh <change-id>"
  exit 1
fi

PROPOSAL_DIR="openspec/changes/$PROPOSAL_ID"

echo "Validating proposal: $PROPOSAL_ID"
echo "====================================="
echo

# Check directory exists
if [ ! -d "$PROPOSAL_DIR" ]; then
  echo "❌ FAIL: Proposal directory not found: $PROPOSAL_DIR"
  exit 1
fi
echo "✓ Directory exists"

# Check required files
if [ ! -f "$PROPOSAL_DIR/proposal.md" ]; then
  echo "❌ FAIL: proposal.md not found"
  exit 1
fi
echo "✓ proposal.md exists"

if [ ! -f "$PROPOSAL_DIR/tasks.md" ]; then
  echo "❌ FAIL: tasks.md not found"
  exit 1
fi
echo "✓ tasks.md exists"

# Check proposal.md sections
echo
echo "Checking proposal.md sections..."
REQUIRED_SECTIONS=("Overview" "Context" "Problem" "Solution" "Scope" Impact")
for section in "${REQUIRED_SECTIONS[@]}"; do
  if grep -q "^## $section" "$PROPOSAL_DIR/proposal.md"; then
    echo "✓ Section '$section' exists"
  else
    echo "❌ FAIL: Section '$section' missing"
  fi
done

# Check header metadata
echo
echo "Checking proposal.md metadata..."
if grep -q "^\\*\\*Status\\*\\*:" "$PROPOSAL_DIR/proposal.md"; then
  echo "✓ Status field exists"
else
  echo "❌ FAIL: Status field missing"
fi

if grep -q "^\\*\\*Type\\*\\*:" "$PROPOSAL_DIR/proposal.md"; then
  echo "✓ Type field exists"
else
  echo "❌ FAIL: Type field missing"
fi

# Check tasks.md structure
echo
echo "Checking tasks.md structure..."
if grep -q "^## Phase" "$PROPOSAL_DIR/tasks.md"; then
  echo "✓ Phase structure exists"
else
  echo "⚠ WARNING: No phase structure found"
fi

TASK_COUNT=$(grep -c "^### [0-9]\+\.[0-9]\+" "$PROPOSAL_DIR/tasks.md" || echo "0")
echo "✓ Found $TASK_COUNT tasks"

# Check for spec files
echo
echo "Checking for spec files..."
if [ -d "$PROPOSAL_DIR/specs" ]; then
  SPEC_COUNT=$(find "$PROPOSAL_DIR/specs" -name "spec.md" | wc -l)
  echo "✓ Found $SPEC_COUNT spec file(s)"
else
  echo "ℹ No specs directory (optional)"
fi

echo
echo "====================================="
echo "Validation complete!"
echo "Review warnings and failures above."
```

Save as `openspec/scripts/validate-proposal.sh` and run:

```bash
chmod +x openspec/scripts/validate-proposal.sh
./openspec/scripts/validate-proposal.sh add-vpdmorph-support
```

## Validation Outcomes

### Pass

Proposal is ready for review when:
- All required files exist
- All required sections are present
- Format requirements are met
- No major issues found

**Action**: Update status to "Review" and notify maintainers

### Pass with Warnings

Proposal has minor issues but is reviewable:
- Optional content missing
- Minor formatting issues
- Clarifications recommended

**Action**: Address warnings if possible, or submit as-is with note

### Fail

Proposal has blocking issues:
- Missing required files or sections
- Format violations
- Incomplete content

**Action**: Address failures before submitting for review

## Common Validation Failures

### Missing proposal.md Sections

**Problem**: Required section header not found

**Fix**: Add missing section with content

```markdown
## Solution

[Describe your proposed approach here]
```

### Missing Header Metadata

**Problem**: proposal.md doesn't have Status/Type/Author fields

**Fix**: Add header at top of proposal.md

```markdown
# [Title]

**Status**: Draft
**Type**: Feature
**Author**: @username
**Created**: 2025-01-20
```

### Tasks Not Actionable

**Problem**: Task description is vague

**Fix**: Make tasks specific and verifiable

**Bad**:
```markdown
### 1.1 Implement feature
- Implement the feature
```

**Good**:
```markdown
### 1.1 Implement VPDMorph parser
- [ ] Create .vpd binary format parser
- [ ] Extract vertex offsets from file
- [ ] Handle format version differences

**Description**: Parse VPDMorph files to extract vertex animation data

**Files**:
- rust_engine/src/format/vpd_parser.rs (new)
- rust_engine/src/format/mod.rs (modify)

**Validation**: Parser successfully loads test.vpd file
```

### Missing Validation Criteria

**Problem**: Task has no validation section

**Fix**: Add **Validation** field to each task

```markdown
**Validation**: Running tests with test model shows morph applied correctly
```

### Spec Format Issues

**Problem**: spec.md doesn't use correct format

**Fix**: Use ADDED/MODIFIED/REMOVED with scenarios

```markdown
## ADDED Requirements

### Requirement: VPDMorph support

The system MUST support VPDMorph format.

#### Scenario: Load VPDMorph file

**Given** a .vpd file exists
**When** the file is loaded
**Then** vertex offsets are extracted
**And** morph is available in UI
```

## Validation Workflow

```
Author completes proposal
  ↓
Run validation checklist
  ↓
Any failures?
  Yes → Fix issues → Re-validate
  No
  ↓
Any warnings?
  Yes → Optionally fix → Re-validate
  No
  ↓
Proposal passes validation
  ↓
Update status to "Review"
  ↓
Submit for maintainer review
```

## Resources

- **Authoring Guide**: `openspec/docs/AUTHORING_GUIDE.md`
- **Reviewer Guide**: `openspec/docs/REVIEWER_GUIDE.md`
- **Conventions**: `openspec/AGENTS.md`
- **Templates**: `openspec/templates/`
