# AI Development and Review Metrics

This is an experiment log, not a scorecard. Record facts from tool output; leave an unavailable field as `UNAVAILABLE` rather than estimating it.

## Per-task record

| Date | Jira / scope | Baseline | Mode | Codex agents | Claude round | Test result | Confirmed defects found | False positives | Codex usage / cost | Claude usage / cost | Elapsed time | Decision / notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 2026-08-31 | Workflow restored after rollback | `95d058c` | Baseline setup | navigator, analyst, contract | not invoked | not applicable | 0 | 0 | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE | No product-code change; first comparable feature run pending. |
| 2026-09-01 | GM-31: 내 게임 프로필 조회 | `95d058c` | Claude direct review | implementer, test engineer, independent reviewer | round 1 | `./gradlew.bat test --no-daemon`; 18 XML reports, no failure/error entries | 1 low: redundant GameUser re-query (ACCEPT) | 0 | Codex: UNAVAILABLE | Claude: input 16,731 + cache 28,467; output 7,690; $0.1951278 | 83.2s | Weekly remaining quotas are not exposed. Claude was read-only, found no contract/security defect; adapter query cleanup dispatched. |
| 2026-09-01 | GM-31: adapter query cleanup | `a73aaf7` | Claude finding remediation | implementer, test engineer, independent reviewer | no second Claude round needed | `./gradlew.bat test --no-daemon`; 18 suites / 85 tests / 0 failures / 0 errors | 1 accepted Claude finding resolved | 0 | UNAVAILABLE | UNAVAILABLE | UNAVAILABLE | Weekly remaining quotas are not exposed. Test engineer and independent reviewer both approved the one-file refactor; behavior is unchanged and redundant GameUser lookup is removed. |
| 2026-09-01 | Next API discovery | `3085b6d` | Planning preflight | navigator pending | not invoked | not applicable | pending | pending | UNAVAILABLE | UNAVAILABLE | pending | Weekly remaining quotas are not exposed; navigation is read-only and selects no API contract without human approval. |
| 2026-09-01 | GameUser search contract analysis | `3085b6d` | Planning preflight | backend analyst pending | not invoked | not applicable | pending | pending | UNAVAILABLE | UNAVAILABLE | pending | Weekly remaining quotas are not exposed; analysis is read-only and does not create an implementation scope without human approval. |
| 2026-09-01 | GameUser search API contract | `3085b6d` | Contract preflight | API contract agent pending | not invoked | not applicable | pending | pending | UNAVAILABLE | UNAVAILABLE | pending | Weekly remaining quotas are not exposed; the contract requires human approval before any implementation scope is created. |

## 2026-11-23 GM-34 closure

- Baseline3b8ee99, existing human-authored tracked changes; analyst, independent test engineer and independent reviewer separated.
- Executed tests: `.\gradlew.bat test --rerun-tasks`, exit0,35s,148 tests,failures/errors/skipped0.
- Claude direct static review: valid structured_output,3 findings,ACCEPT0/REJECT3; adjudication in `docs/GM-34-api-end-review.md`.
- Claude usage: input2/cache creation23162/cache read0/output16351; $0.27392;168.217s.
- Codex pre-dispatch weekly remaining91%,5h44%; pre-Claude weekly90%,5h38%; Claude weekly quota UNAVAILABLE. No reset/credits used.
- Explicit human approval covered sending GM-34 requirement/diff/context/test evidence to Anthropic after automatic approval review initially blocked it.
- Integration into GM-35:192 tests passed,40s; duplicate shared JSON handler removed; independent integration review found no further issue.
- Actual MySQL concurrency/snapshot validation was not performed. No main merge or deployment.

## Comparison rules

Use comparable tasks or known-defect cases. Hold the requirement, Git baseline, acceptance criteria, test command, and review time budget fixed. Track:

- task success and required-evidence completeness;
- confirmed defects found and defects missed;
- false positives;
- input/output tokens or provider-reported usage and cost;
- elapsed time, number of calls, and rework rounds.

Review routing is a hypothesis, not a conclusion: use Claude only where the measured added defect detection justifies its latency and usage.
