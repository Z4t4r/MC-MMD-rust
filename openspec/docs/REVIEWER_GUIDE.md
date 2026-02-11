# OpenSpec Proposal Reviewer Guide

This guide helps maintainers review change proposals for the MC-MMD-rust project.

## Review Process Overview

### 1. Initial Assessment

When a proposal is marked "Review":

1. Read the proposal.md to understand what's being proposed
2. Check tasks.md for implementation plan
3. Review spec.md (if present) for requirement changes
4. Assess whether the proposal is ready for detailed review

### 2. Detailed Review

Evaluate the proposal against the criteria in this guide.

### 3. Provide Feedback

- Comment on areas needing improvement
- Ask clarifying questions
- Suggest alternatives if appropriate
- Either request changes or approve

### 4. Approve or Request Changes

- **Approve**: If proposal meets all criteria
- **Request changes**: If specific issues need addressing
- **Reject**: If proposal is fundamentally flawed (rare)

## Review Criteria

### Clarity

The proposal should be clear and understandable.

**Check**:
- [ ] Overview clearly explains what the change does
- [ ] Problem statement is specific and well-defined
- [ ] Solution approach is understandable
- [ ] Language is precise and unambiguous
- [ ] Technical jargon is explained or linked

**Red flags**:
- Vague descriptions like "improve performance"
- Missing context about why this change is needed
- Unclear what "done" looks like

**Questions to ask**:
- Can I explain this proposal to someone else in 2 minutes?
- Would a new contributor understand what this does and why?

### Completeness

The proposal should have all required information.

**Check**:
- [ ] All proposal.md sections are filled out
- [ ] tasks.md has actionable, ordered tasks
- [ ] Scope clearly defines in/out items
- [ ] Impact assessment covers benefits and risks
- [ ] Success criteria are measurable

**Red flags**:
- Empty or minimal sections
- "TBD" or "TODO" placeholders in core sections
- Missing validation criteria in tasks

### Feasibility

The proposal should be realistically implementable.

**Check**:
- [ ] Tasks are broken down sufficiently
- [ ] Required effort is reasonable for the benefit
- [ ] Technical approach is sound
- [ ] Dependencies are identified
- [ ] Required knowledge/skills are available

**Red flags**:
- One massive task with no breakdown
- Unrealistic performance claims
- Missing key technical details
- Dependencies on unclear external factors

**Questions to ask**:
- Can this be implemented with available resources?
- Are there any hidden complexities not addressed?

### Impact

The proposal should accurately assess its effects.

**Check**:
- [ ] Benefits are clearly stated
- [ ] Affected components are listed
- [ ] Compatibility is addressed
- [ ] Breaking changes are documented (if any)
- [ ] Migration path is clear (if needed)
- [ ] Risks are identified with mitigations

**Red flags**:
- No discussion of potential downsides
- Breaking changes without migration plan
- Over-optimistic benefit claims

### Alternatives

The proposal should consider alternative approaches.

**Check**:
- [ ] At least one alternative is considered
- [ ] Rationale for chosen approach is clear
- [ ] Trade-offs are acknowledged
- [ ] Why alternatives were not chosen is explained

**Red flags**:
- No discussion of alternatives
- "This is the only way" without justification
- Better alternatives not considered

## Proposal Type-Specific Criteria

### Feature Proposals

**Additional checks**:
- [ ] User benefit is clear
- [ ] API changes are documented
- [ ] Configuration options (if any) are specified
- [ ] Backwards compatibility is addressed

**Common issues**:
- Feature scope creep (too much in one proposal)
- Missing user-facing documentation plan
- No consideration of performance impact

### Refactor Proposals

**Additional checks**:
- [ ] Current problems are clearly described
- [ ] New structure is well-defined
- [ ] No behavioral changes (or they're explicit)
- [ ] Testing strategy ensures no regressions

**Common issues**:
- "Refactor" that actually changes behavior
- Incomplete testing plan
- Missing migration plan for existing code

### Performance Proposals

**Additional checks**:
- [ ] Current performance is measured (baseline)
- [ ] Target performance is specified
- [ ] Benchmark methodology is described
- [ ] Why optimization requires structural change

**Common issues**:
- No baseline measurements
- Unrealistic performance claims
- Missing benchmark methodology
- Optimization could be simpler

### Architecture Proposals

**Additional checks**:
- [ ] Current and proposed architecture diagrams
- [ ] Rationale for structural change
- [ ] Migration strategy is complete
- [ ] Rollback plan exists
- [ ] Impact on other components is considered

**Common issues**:
- Missing architecture diagrams
- Incomplete migration plan
- No rollback strategy
- Insufficient consideration of edge cases

### Bugfix Proposals

**Additional checks**:
- [ ] Root cause is identified
- [ ] Why fix is safe is explained
- [ ] Regression testing is planned
- [ ] How similar bugs will be prevented

**Common issues**:
- Symptom fix without addressing root cause
- Insufficient testing for edge cases
- No consideration of similar bugs elsewhere

## Review Checklist

Use this checklist when reviewing a proposal:

### proposal.md Review

| Section | Criteria | Pass/Fail | Notes |
|---------|----------|-----------|-------|
| Header | Status, Type, Author, Created filled | | |
| Overview | Concise summary (2-3 sentences) | | |
| Context | Sufficient background information | | |
| Problem | Clear, specific problem statement | | |
| Solution | Well-described approach | | |
| Scope | In/out items clearly defined | | |
| Impact | Benefits, risks, compatibility addressed | | |
| Success Criteria | Measurable outcomes listed | | |
| Alternatives | At least one alternative considered | | |

### tasks.md Review

| Criteria | Pass/Fail | Notes |
|----------|-----------|-------|
| Tasks are ordered logically | | |
| Each task is actionable | | |
| Each task has validation criteria | | |
| Dependencies are documented | | |
| Tasks are grouped in phases | | |
| No vague tasks | | |

### spec.md Review (if present)

| Criteria | Pass/Fail | Notes |
|----------|-----------|-------|
| Uses ADDED/MODIFIED/REMOVED headers | | |
| Each requirement has scenarios | | |
| Scenarios use Given-When-Then | | |
| Related capabilities cross-referenced | | |

## Providing Feedback

### Feedback Best Practices

1. **Be specific**: Point to exact sections needing work
2. **Be constructive**: Explain what needs improvement and why
3. **Be respectful**: Remember the author invested time
4. **Prioritize**: Identify must-fix vs. nice-to-have
5. **Offer alternatives**: Suggest improvements

### Feedback Template

```markdown
## Review Feedback for <proposal-id>

### Overall Assessment

[Accept / Accept with changes / Request changes / Reject]

### Strengths

- [What's good about the proposal]
- [Clear areas that are well done]

### Required Changes

Must address before approval:

1. **[Section Name]**
   - Issue: [Description]
   - Suggestion: [How to fix]

2. **[Section Name]**
   - Issue: [Description]
   - Suggestion: [How to fix]

### Suggested Improvements

Optional improvements that would strengthen the proposal:

1. **[Section Name]**
   - Suggestion: [Improvement idea]
   - Rationale: [Why this would help]

### Questions

Clarifications needed:

- [Question 1]
- [Question 2]

### Overall Comments

[Any general feedback, encouragement, etc.]
```

## Approval

### When to Approve

Approve when:
- All required changes are addressed
- All acceptance criteria are met
- You're confident the proposal can be implemented successfully
- Benefits justify the costs/risks

### How to Approve

Add approval metadata to proposal.md:

```markdown
**Status**: Approved
**Type**: Feature
**Author**: @username
**Created**: 2025-01-15
**Approved**: @maintainer-name
**Approval Date**: 2025-01-20
**Approved Notes**: [Any conditions or concerns]
**Completed**: [Date when complete]
```

### Conditional Approval

You may approve with conditions:

```markdown
**Approved Notes**: Approved with the following conditions:
1. Performance must be benchmarked before merge
2. Documentation must be updated before merge
3. Migration guide must be included
```

## Common Review Findings

### Incomplete Proposals

**Symptoms**: Missing sections, TBD placeholders, minimal content

**Action**: Request completion, specify what's missing

### Overscoped Proposals

**Symptoms**: Too many unrelated changes, massive task list

**Action**: Suggest splitting into multiple proposals

### Underscoped Proposals

**Symptoms**: Missing key tasks, incomplete implementation plan

**Action**: Point out missing work, ask for complete task breakdown

### Vague Success Criteria

**Symptoms**: "Works better", "Improved", "Fixed"

**Action**: Request specific, measurable outcomes

### Missing Risk Assessment

**Symptoms**: No discussion of what could go wrong

**Action**: Ask for risk analysis and mitigation strategies

### Unjustified Approach

**Symptoms**: Chose solution without considering alternatives

**Action**: Ask for alternative analysis and rationale

## Review Workflow

```
Proposal marked "Review"
  ↓
Initial assessment (5-10 minutes)
  ↓
Ready for detailed review?
  No → Request improvements → Author revises
  ↓ Yes
Detailed review (30-60 minutes)
  ↓
Provide feedback
  ↓
Author addresses feedback
  ↓
Re-review if needed
  ↓
All criteria met?
  No → Request more changes
  ↓ Yes
Approve proposal
  ↓
Proposal moves to "Approved" state
```

## Tips for Efficient Review

1. **Skim first**: Get overview before diving deep
2. **Take notes**: Mark issues as you find them
3. **Check templates**: Verify format requirements first
4. **Focus on impact**: Is this change worth the effort?
5. **Trust but verify**: Don't assume author is correct
6. **Ask early**: If something is unclear, ask immediately

## Escalation

If you encounter:

- **Fundamentally flawed approach**: Reject and explain why
- **Outside project scope**: Redirect to appropriate venue
- **Duplicate effort**: Point to existing proposal/work
- **Requires broader discussion**: Suggest team discussion

## Resources

- **Authoring Guide**: `openspec/docs/AUTHORING_GUIDE.md`
- **Conventions**: `openspec/AGENTS.md`
- **Templates**: `openspec/templates/`
- **Project Context**: `openspec/project.md`
