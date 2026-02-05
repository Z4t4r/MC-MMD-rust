Stage changes and create a git commit. Use the user's input as the commit message when they provide one.

## User input

The text the user typed after invoking this command is the commit message (or message prefix). You **MUST** use it when non-empty.

## Outline

1. **Verify repository**: From repo root, run `git rev-parse --git-dir 2>$null`. If this fails, stop and report that the current folder is not a git repository.

2. **Show status**: Run `git status --short` and, if useful, `git diff --stat` (or `git diff --cached --stat` for staged changes). Summarize what is staged vs unstaged.

3. **Stage changes** (if needed):
   - If the user input implies scope (e.g. "only src/", "MaterialClient project"), stage only those paths.
   - Otherwise run `git add -A` (or on Windows PowerShell: `git add --all`) to stage all changes.
   - If there is nothing to commit (no changes and nothing staged), stop and report.

4. **Commit message**:
   - If the user provided a message: use it. If it does not look like a full message (e.g. single word), you may suggest a fuller message but still use the user input unless they asked for a suggestion.
   - If the user provided nothing: propose a short commit message based on the staged changes (e.g. from `git diff --cached --name-only` and diff summary), then run commit with that message. Prefer conventional style (e.g. `feat:`, `fix:`, `chore:`) when it fits the changes.

5. **Create commit**: Run `git commit -m "<message>"` (escape the message appropriately for the shell; prefer double-quoted messages and escape internal double quotes). On Windows PowerShell, use single quotes for the message if it contains no single quotes, otherwise escape as needed.

6. **Confirm**: Run `git log -1 --oneline` and report the new commit hash and subject.

## Rules

- Use absolute paths for the repo root when running scripts (e.g. workspace root).
- Do not run `git push` unless the user explicitly asks to push.
- If the user only wants to stage and not commit, or only wants a suggested message, follow their instruction and do not run `git commit` in that case.
