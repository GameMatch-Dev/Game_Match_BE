# AI Development and Review Metrics

This is an experiment log, not a scorecard. Record facts from tool output; leave an unavailable field as `UNAVAILABLE` rather than estimating it.

## Per-task record

### 2026-11-24 참가 신청 API 종료 검증

- 기준: main3425093, 참가 신청 초기 구현c9db48c. 실제 JWT HTTP 통합17개 통과.
- Codex 독립 리뷰는 재신청 경합을 지적했다. H2 독립 트랜잭션 테스트에서 성공1건을 기대했지만2건 모두 성공하는 RED를 확인했다. 조건부 UPDATE/refresh 수정 뒤 HTTP17+경합1+Persistence17=35개 통과.
- Claude 읽기 전용 직접 리뷰: 유효 structured_output4건. ACCEPT2(동시 재신청, 팀원별 조회 증가), REJECT2(응답 PENDING 추가, 이번 범위의 FK 차단 요구). 경합은 Codex와 중복된 동일 결함이므로 두 결함으로 합산하지 않는다.
- Claude 비용$0.273115,139.607초,input2/cache creation30513/cache read0/output12773(thinking10833). Claude 주간 잔여 UNAVAILABLE. 호출 전 Codex 주간 잔여85%,5시간 잔여4%. 사용자가 이번 자료의 Anthropic 전송을 명시 승인했다. 리셋/크레딧 사용 없음.
- 일괄 조회 테스트 선행 컴파일 RED8errors 후 대상62개 GREEN. 실제 SQL은 영속성 문맥 캐시의 영향을 받으므로 이전 저장소3N호출을 3N SQL이라고 단정하지 않는다. 냉각된 프로필3개 일괄 조회 최대3쿼리, 빈 입력0쿼리 회귀 테스트를 추가했다.
- Claude 수정 후 재호출은 하지 않았다. 두 수정에 대한 독립 Codex 재검토 추가 findings 없음. 항목별 근거와 한계는 `docs/match-request-api-end-review.md` 참조.
- 최종 전체 회귀:267개,실패0/오류0/생략0,exit0,Gradle39초(도구39.535초). 기존 생성 API 통합6개도 포함한다. MySQL 실행 검증을 뜻하지 않는다.

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

## 2026-11-23 GM-35 API end review

Human approved this API review and remediation despite weekly Codex37%, ClaudeUNAVAILABLE.
Codex independent review identified P2 fractional JSON coercion; HTTP reproduction returned201
for four fractional integer fields. ACCEPT and fixed using accept-float-as-int:false in main/test.
Separate test engineer identified TEAM/INDIVIDUAL persistence mapping and past datetime HTTP400/DB0
coverage gaps; both ACCEPT and implemented. Targeted integration6 pass; WebMvc26 pass.
Final lead execution `.\gradlew.bat test` exit0,31s,184 tests,failures/errors0.
Test agent subsequently reached actual provider limit. No reset used. Codex final post-fix independent
review not executed. Claude calls below were approved, tool-free and no-session-persistence.

| Attempt | Result | Provider reported usage | Total cost | CLI time |
| --- | --- | --- | --- | --- |
| Direct | Empty result; incomplete, no findings adjudication possible | input2/cache creation51170/cache read3293/output13762 | $0.3806556 | 155.405s |
| Compact retry | Tool-call request text instead of findings; incomplete; tools not executed | input2/cache creation12540/cache read3293/output2965 | $0.0876626 | 34.809s |

No valid Claude defect report or clean review was received. Stop repeated calls; API review completion
is pending. Raw outputs: task-local gm35-final-review/claude.json and claude-retry.json.
Codex last exposed weekly remaining34%, ClaudeUNAVAILABLE; per-agent token usageUNAVAILABLE.

### Diagnostic recovery and scoped remediation

After CLI diagnosis, explicit static-review system prompt with dontAsk, tools disabled,
safe-mode and JSON Schema returned valid structured findings on the real API input.
Cost$0.152864,CLI114.663s; input2/cache creation9460/cache read0/output10792.
ACCEPT Medium global coercion blast radius: reverted global YAML change and restricted float
rejection to CreateFriendlyMatchRequest fields via Jackson3 deserializers.
ACCEPT Low ordinal response mapping fragility: explicit exhaustive response switches replace ordinal.
REJECT only the finding's overstatement that current fixed-value mapping integration tests would
necessarily all pass after enum reorder; tests assert API numeric values directly.
User explicitly requested reuse of completed Codex review; no full Codex re-review performed.
Latest full regression after both remediations184 pass,exit0,35s.
Scoped Claude re-review returned one Medium finding requesting integer strictness in other APIs.
REJECT as scope expansion/reversal of prior accepted scoped remediation: the old pre-task baseline
had no global strictness guarantee. No evidence of regression in the four fixed create fields.
No residual ordinal finding. All original accepted findings resolved; no clean/empty-report claim.
Re-review cost$0.111077,CLI108.864s,input2/cache creation3241/cache read0/output9543.

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
