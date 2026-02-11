#!/bin/bash
# show-proposal.sh - Display detailed proposal information

PROPOSAL_ID=$1

if [ -z "$PROPOSAL_ID" ]; then
  echo "Usage: ./show-proposal.sh <change-id>"
  exit 1
fi

PROPOSAL_DIR="openspec/changes/$PROPOSAL_ID"

if [ ! -d "$PROPOSAL_DIR" ]; then
  echo "Proposal not found: $PROPOSAL_ID"
  exit 1
fi

echo "========================================"
echo "Proposal: $PROPOSAL_ID"
echo "========================================"
echo

# Show proposal.md header
if [ -f "$PROPOSAL_DIR/proposal.md" ]; then
  echo "--- Metadata ---"
  grep "^\\*\\*" "$PROPOSAL_DIR/proposal.md" | head -10
  echo

  # Show overview
  echo "--- Overview ---"
  awk '/^## Overview/,/^## [^O]/' "$PROPOSAL_DIR/proposal.md" | head -10 | grep -v "^##"
  echo
fi

# Show tasks summary
if [ -f "$PROPOSAL_DIR/tasks.md" ]; then
  echo "--- Tasks ---"
  grep "^## Phase" "$PROPOSAL_DIR/tasks.md"
  task_count=$(grep -c "^### [0-9]" "$PROPOSAL_DIR/tasks.md" || echo "0")
  echo "Total tasks: $task_count"
  echo
fi

# Show specs
if [ -d "$PROPOSAL_DIR/specs" ]; then
  echo "--- Specs ---"
  find "$PROPOSAL_DIR/specs" -name "spec.md" -exec echo "Found: {}" \;
  echo
fi

echo "========================================"
echo "Files:"
ls -la "$PROPOSAL_DIR/"
