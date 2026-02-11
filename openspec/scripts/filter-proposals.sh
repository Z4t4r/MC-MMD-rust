#!/bin/bash
# filter-proposals.sh - Filter and list proposals by status or type

FILTER_TYPE=$1
FILTER_VALUE=$2

PROPOSALS_DIR="openspec/changes"

show_help() {
  echo "Usage: ./filter-proposals.sh <filter-type> <value>"
  echo
  echo "Filter types:"
  echo "  status <Draft|Review|Approved|Implemented|Completed>"
  echo "  type <Feature|Refactor|Performance|Architecture|Bugfix>"
  echo
  echo "Examples:"
  echo "  ./filter-proposals.sh status Review    # Show all Review proposals"
  echo "  ./filter-proposals.sh type Feature     # Show all Feature proposals"
  echo "  ./filter-proposals.sh status Approved  # Show all Approved proposals"
  exit 1
}

if [ -z "$FILTER_TYPE" ] || [ -z "$FILTER_VALUE" ]; then
  show_help
fi

if [ "$FILTER_TYPE" != "status" ] && [ "$FILTER_TYPE" != "type" ]; then
  echo "Error: Filter type must be 'status' or 'type'"
  echo
  show_help
fi

echo "Proposals with $FILTER_TYPE = $FILTER_VALUE"
echo "============================================"
echo

found=0

for proposal_dir in "$PROPOSALS_DIR"/*/; do
  if [ -d "$proposal_dir" ]; then
    proposal_id=$(basename "$proposal_dir")
    proposal_file="$proposal_dir/proposal.md"

    if [ -f "$proposal_file" ]; then
      # Extract the field value
      if [ "$FILTER_TYPE" = "status" ]; then
        value=$(grep "^\\*\\*Status\\*\\*:" "$proposal_file" | sed 's/.*\*\*Status\*\*: //' | sed 's/[\*]*$//' | head -1 | xargs)
      else
        value=$(grep "^\\*\\*Type\\*\\*:" "$proposal_file" | sed 's/.*\*\*Type\*\*: //' | sed 's/[\*]*$//' | head -1 | xargs)
      fi

      # Case-insensitive comparison
      if [[ "${value,,}" == "${FILTER_VALUE,,}" ]]; then
        found=$((found + 1))
        title=$(head -1 "$proposal_file" | sed 's/^# //')
        author=$(grep "^\\*\\*Author\\*\\*:" "$proposal_file" | sed 's/\*\*Author\*\*: //' | head -1 | xargs)
        created=$(grep "^\\*\\*Created\\*\\*:" "$proposal_file" | sed 's/\*\*Created\*\*: //' | head -1 | xargs)

        echo "[$found] $proposal_id"
        echo "    Title: $title"
        echo "    Author: $author"
        echo "    Created: $created"
        echo
      fi
    fi
  fi
done

if [ $found -eq 0 ]; then
  echo "No proposals found with $FILTER_TYPE = $FILTER_VALUE"
else
  echo "============================================"
  echo "Found $found proposal(s)"
fi
