#!/bin/bash
# list-proposals.sh - List all OpenSpec proposals with summary

PROPOSALS_DIR="openspec/changes"

echo "OpenSpec Proposals"
echo "=================="
echo

if [ ! -d "$PROPOSALS_DIR" ]; then
  echo "No proposals directory found"
  exit 1
fi

# Count proposals
count=0
for proposal_dir in "$PROPOSALS_DIR"/*/; do
  if [ -d "$proposal_dir" ]; then
    count=$((count + 1))
  fi
done

echo "Total proposals: $count"
echo

# List each proposal
for proposal_dir in "$PROPOSALS_DIR"/*/; do
  if [ -d "$proposal_dir" ]; then
    proposal_id=$(basename "$proposal_dir")
    proposal_file="$proposal_dir/proposal.md"

    if [ -f "$proposal_file" ]; then
      # Extract metadata
      title=$(head -1 "$proposal_file" | sed 's/^# //')
      status=$(grep "^\\*\\*Status\\*\\*:" "$proposal_file" | sed 's/\*\*Status\*\*: //' | head -1)
      type=$(grep "^\\*\\*Type\\*\\*:" "$proposal_file" | sed 's/\*\*Type\*\*: //' | head -1)
      author=$(grep "^\\*\\*Author\\*\\*:" "$proposal_file" | sed 's/\*\*Author\*\*: //' | head -1)
      created=$(grep "^\\*\\*Created\\*\\*:" "$proposal_file" | sed 's/\*\*Created\*\*: //' | head -1)

      # Default values if not found
      status=${status:-"Unknown"}
      type=${type:-"Unknown"}
      author=${author:-"Unknown"}
      created=${created:-"Unknown"}

      # Print proposal info
      echo "----------------------------------------"
      echo "ID: $proposal_id"
      echo "Title: $title"
      echo "Status: $status"
      echo "Type: $type"
      echo "Author: $author"
      echo "Created: $created"
    else
      echo "----------------------------------------"
      echo "ID: $proposal_id"
      echo "Warning: proposal.md not found"
    fi
  fi
done

echo
echo "=================="
