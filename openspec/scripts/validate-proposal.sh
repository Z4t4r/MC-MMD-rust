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
  echo "FAIL: Proposal directory not found: $PROPOSAL_DIR"
  exit 1
fi
echo "PASS: Directory exists"

# Check required files
if [ ! -f "$PROPOSAL_DIR/proposal.md" ]; then
  echo "FAIL: proposal.md not found"
  exit 1
fi
echo "PASS: proposal.md exists"

if [ ! -f "$PROPOSAL_DIR/tasks.md" ]; then
  echo "FAIL: tasks.md not found"
  exit 1
fi
echo "PASS: tasks.md exists"

# Check proposal.md sections
echo
echo "Checking proposal.md sections..."
REQUIRED_SECTIONS=("Overview" "Context" "Problem" "Solution" "Scope")
for section in "${REQUIRED_SECTIONS[@]}"; do
  if grep -q "^## $section" "$PROPOSAL_DIR/proposal.md"; then
    echo "PASS: Section '$section' exists"
  else
    echo "FAIL: Section '$section' missing"
  fi
done

# Check for Impact or Impact Assessment
if grep -q "^## Impact" "$PROPOSAL_DIR/proposal.md" || grep -q "^## Impact Assessment" "$PROPOSAL_DIR/proposal.md"; then
  echo "PASS: Section 'Impact' exists"
else
  echo "FAIL: Section 'Impact' missing"
fi

# Check header metadata
echo
echo "Checking proposal.md metadata..."
if grep -q "^\\*\\*Status\\*\\*:" "$PROPOSAL_DIR/proposal.md"; then
  echo "PASS: Status field exists"
else
  echo "FAIL: Status field missing"
fi

if grep -q "^\\*\\*Type\\*\\*:" "$PROPOSAL_DIR/proposal.md"; then
  echo "PASS: Type field exists"
else
  echo "FAIL: Type field missing"
fi

# Check tasks.md structure
echo
echo "Checking tasks.md structure..."
if grep -q "^## Phase" "$PROPOSAL_DIR/tasks.md"; then
  echo "PASS: Phase structure exists"
else
  echo "WARN: No phase structure found"
fi

TASK_COUNT=$(grep -c "^### [0-9]\+\.[0-9]\+" "$PROPOSAL_DIR/tasks.md" 2>/dev/null || echo "0")
echo "INFO: Found $TASK_COUNT tasks"

# Check for spec files
echo
echo "Checking for spec files..."
if [ -d "$PROPOSAL_DIR/specs" ]; then
  SPEC_COUNT=$(find "$PROPOSAL_DIR/specs" -name "spec.md" 2>/dev/null | wc -l)
  echo "INFO: Found $SPEC_COUNT spec file(s)"
else
  echo "INFO: No specs directory (optional)"
fi

echo
echo "====================================="
echo "Validation complete!"
echo "Review warnings and failures above."
