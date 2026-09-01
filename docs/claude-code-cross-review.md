# Claude Code Cross-Model Review

Claude is an independent **read-only reviewer**, never an implementer, tester of record, Git/Jira/GitHub writer, or decision maker.

## Entry conditions

Run one review only after all of these exist:

1. Authoritative Jira requirement and approved API/policy decisions.
2. Current task diff and relevant code context.
3. Actual test command, exit code, duration, and output summary.
4. Codex independent-review report.
5. The weekly usage guard has been recorded.

## Invocation rules

- Use `tools/invoke-claude-readonly-review.ps1` and the fixed template in `docs/templates/claude-code-review-prompt.md`.
- Claude receives the requirement, diff, selected context, and test evidence only. Do not pass the full Codex conversation or self-review reasoning.
- The helper runs Claude with no tools, plan permission mode, and no persisted session.
- Start with one direct review. A re-review is optional and limited to two rounds.

## Finding gate

Codex evaluates every item against the requirement, current diff, and executed-test evidence:

| Decision | Meaning |
| --- | --- |
| `ACCEPT` | A real actionable defect or missing evidence; dispatch only the required remediation role. |
| `REJECT` | Incorrect, out of scope, duplicate, or unsupported; record the rationale. |
| `UNRESOLVED` | Needs a human policy decision; do not silently implement it. |

No merge or completion claim is allowed while an accepted high-severity finding remains unresolved.

## Measurement

For every review, append one row to `docs/ai-review-metrics.md`. Compare Codex-only, Claude direct, and (only when justified) Claude blind-plus-review using the same requirement, baseline, test evidence, and time budget. Lower usage is an improvement only when quality evidence is equal or better.
