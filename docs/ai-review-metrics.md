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

## Comparison rules

Use comparable tasks or known-defect cases. Hold the requirement, Git baseline, acceptance criteria, test command, and review time budget fixed. Track:

- task success and required-evidence completeness;
- confirmed defects found and defects missed;
- false positives;
- input/output tokens or provider-reported usage and cost;
- elapsed time, number of calls, and rework rounds.

Review routing is a hypothesis, not a conclusion: use Claude only where the measured added defect detection justifies its latency and usage.

## 2026-11-23 JWT Resource Server migration

Scope: user-approved replacement of custom JWT filter with Spring Resource Server; excludes hostGameUserId validation and usage-policy edits. Human explicitly approved this review batch below the local usage threshold.

| Stage | Evidence / result | Usage | Duration |
| --- | --- | --- | --- |
| Codex independent review | No actionable implementation defect | Weekly remaining before 44%, after batch 42%; token usage UNAVAILABLE | UNAVAILABLE |
| Separate test engineer | XML 160 tests, 0 failures/errors/skips; ACCEPT P2 missing expiry and issuer HTTP cases | Same Codex quota; token usage UNAVAILABLE | UNAVAILABLE |
| Remediation and independent recheck | Added two tests; targeted JWT suite 9 pass, exit 0; no new findings | Same approved scope | Gradle 20s |
| Claude direct attempt 1 | Incomplete output without findings JSON; not a completed review | Reported input 2, cache creation 16,599, cache read 3,293, output 3,265; total cost $0.1096226 | CLI 37.075s |
| Claude direct retry | findings: []; no standalone actionable finding | Reported input 2, cache creation 14,334, cache read 5,652, output 9,939; total cost $0.1678384 | CLI 100.943s |

Product regression evidence: `.\gradlew.bat test`, exit 0, 37s, 160 tests pass before the two test-only additions. Latest targeted execution: JWT suite 9 pass. Do not report a newly executed 162-test full suite. Claude reviewed the unchanged product diff and pre-addition test evidence; the final two tests were reviewed independently by Codex. Provider weekly Claude quota UNAVAILABLE. Both Claude calls used the fixed read-only helper with no tools and no persisted session. Raw outputs are in the task-local `jwt-review` folder; CLI total cost includes auxiliary model usage.
