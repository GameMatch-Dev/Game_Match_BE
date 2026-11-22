# GM-35 생성 API 종료 리뷰

2026-11-23. 기준 커밋 764f638 및 아래 미커밋 보강.
사용자가 주간 잔여 Codex 37%, Claude UNAVAILABLE 상태에서 이번 종료 리뷰와
발견 사항 수정을 명시적으로 승인했다. 진행 중 테스트 에이전트가 실제 제공자
한도에 도달해 중단했고, 최종 전체 테스트는 주 담당이 실행했다. 리셋은 사용하지 않았다.

## Codex 판정

- ACCEPT P2: 개인전 sort=1의 실제 저장과 응답 왕복 증거 보강.
  정상 생성 통합 테스트를 TEAM/INDIVIDUAL로 매개변수화하고 DB enum과 응답 숫자를 확인했다.
- ACCEPT P2: 과거 일시 요청의 실제 HTTP 400 및 DB 미저장 증거 보강.
- ACCEPT P2: Jackson이 소수 ID/정원/sort를 정수로 잘라 처리하는 결함.
  MockMvc에서 capacity1.5, sort0.5, hostGameUserId10.5, gameId1.5가 201로 통과함을 확인했다.
  초기에는 main/test YAML에서 accept-float-as-int를 false로 설정했다.
  이후 Claude 지적에 따라 전역 설정을 제거하고 생성 Request의 네 정수 필드에만
  JsonDeserialize를 적용해 소수 토큰을 거부한다.
- REJECT: 2월 30일이 2월 28일로 조용히 바뀐다는 가설은 실제 Jackson3 파싱 재현에서
  InvalidFormatException이 발생해 지지되지 않았다.

## 실행 증거

- 보강 전 전체 178개 통과.
- 팀전/개인전 및 과거 일시 통합 테스트 6개 통과, exit0, 19초.
- 소수 정수 변환 회귀 4건 RED 확인(실제201, 기대400).
- 설정 수정 후 WebMvc26개 통과.
- 최종 `.\gradlew.bat test`: exit0, 31초, 전체184개, 실패/오류0.

## 남은 문서 정합성

Jira GM-35의 recruit와 타인 프로필400 문구는 명시적으로 승인된 최신
capacity/403 및 입력 검증 정책과 동기화해야 한다. 시간대는 승인된 서버 로컬 기준이다.
현재 리뷰만으로 PR/병합/배포 완료를 뜻하지 않는다.

## Claude

첫 도구 없는 읽기 전용 호출은 exit0이나 본문이 빈 문자열이었다.
따라서 유효한 리뷰로 인정하지 않고 최종 소스와 축약된 변경 증빙으로 한 차례 재시도한다.
첫 호출 총비용 $0.3806556, CLI155.405초, input2/cache creation51170/cache read3293/output13762.

재시도도 findings JSON 대신 Task/Explore 호출 요청을 일반 텍스트로 출력했다.
도구가 비활성화돼 실제 도구 실행은 없었다. 이 응답도 유효한 리뷰로 인정하지 않는다.
재시도 총비용 $0.0876626, CLI34.809초,
input2/cache creation12540/cache read3293/output2965.
추가 반복 호출은 중단했다. Claude 완료/무결함 판정은 없으며 이 단계는 미완료다.
소수 입력 수정 뒤 추가 Codex 독립 재검토도 실제 에이전트 한도 때문에 미실행이다.
제품/테스트 변경은 전체184개 통과 상태로 보존하고, 리뷰 보강 커밋은 아직 만들지 않았다.

## Claude 호출 진단 후 유효한 결과와 판정

리뷰 전용 system-prompt, dontAsk(도구 비활성화 유지), json-schema 조합에서
유효한 structured_output.findings 2건을 받았다. 사용자 승인에 따라 기존 Codex
리뷰를 재사용하고 아래 두 변경만 수정·테스트·Claude 부분 재검토한다.

- ACCEPT Medium: 정수 coercion 전역 변경의 영향 범위. 전역 설정을 되돌리고
  FriendlyMatchIntegerDeserializers와 Request 필드 애너테이션으로 API에 한정했다.
- ACCEPT Low: 응답 sort/state의 ordinal 의존. 응답 mapper의 exhaustive switch로
  TEAM0, INDIVIDUAL1, RECRUITING0을 명시했다. 도메인에 HTTP 숫자 코드를 추가하지 않았다.
  단, enum 순서 변경 후에도 현재 테스트가 전부 통과한다는 Claude 설명은 REJECT한다.
  실제 통합 테스트는 요청 sort0/1에 대해 응답 숫자와 저장 enum을 함께 확인한다.

위 두 수정 후 전체 테스트184개 통과, exit0,35초. Codex 전체 리뷰는 반복하지 않았다.
Claude 부분 재검토는 유효한 structured_output을 반환했다.
추가 Medium finding은 '전역 엄격 파싱을 다른 API에도 다시 적용하라'였다.
REJECT: 원래 변경 전 다른 API에 전역 보장이 존재하지 않았으며, 이전 finding의
승인된 해결은 이번 API에 한정해 기존 타 API 동작을 유지하는 것이었다.
다른 API의 소수 정책 확정은 이번 승인 범위를 넘는다. 구체적인 이번 API 회귀 증거도 없다.
ordinal 수정에 대한 잔여 finding은 없었다. 원래 ACCEPT한 두 항목은 수정 완료다.
재검토 비용$0.111077,CLI108.864초,input2/cache creation3241/cache read0/output9543.
최종 상태: 기존 Codex 리뷰 유지, Claude 항목별 판정 완료, 전체184개 통과.
변경은 아직 미커밋이고 원격 Jira 동기화와 PR/CI는 별도 남아 있다.

## 선행 GM-34 커밋 반영 후 회귀 검증

- GM-35 리뷰 반영은 65140c2에 커밋되었고 Draft PR #30을 생성했다.
- GM-34의 4f38821, 3b8ee99를 merge commit e95567e로 반영했다.
- 최초 회귀 실행에서 중복 등록 통합 테스트 1개가 기존 400 기대값 때문에 실패했다.
  승인된 중복 닉네임 계약인 409 GAME_USER_002에 맞춰 기대값만 수정했다.
- 수정 후 전체 `.\gradlew.bat test`: exit0, 37초. XML 집계는 PR 검증 기록에 남긴다.
- 이 단계에서 새 Claude 리뷰나 Codex 독립 리뷰를 실행하지 않았다.
  기존 리뷰 기록을 유지하며 원래 GM-34 작업 폴더의 미커밋 변경은 옮기지 않았다.
- GM-34의 main 병합과 Jira 접근 복구는 여전히 별도 남은 조건이다.

## GM-34 남은 변경 최종 통합

- GM-34의 미커밋 변경은 85132d2/84edf8b/ca7bf8a로 정리했고 PR #31을 생성했다.
  이제 선행 변경 미반영 조건은 해소됐다. 미추적 실험/워크트리 파일은 제외했다.
- GM-35 merge4521538 이후 중복 JSON 처리기만 94de3c3에서 제거했다.
  Resource Server 인증과 사용자 ID 추출은 유지했다. 별도 독립 재검토 findings 없음.
- 전체 테스트 exit0,40초,192개 통과,실패/오류/생략0.
  94de3c3의 Backend CI run45049218696 성공.
- GM-34 자체는 독립 테스트148개 통과 및 Codex 독립 리뷰 완료.
  Claude3건은 개별 REJECT로 판정했고 `docs/GM-34-api-end-review.md`에 근거와 비용을 기록했다.
  이 GM-34 리뷰를 GM-35 전체의 새 Claude 재리뷰로 세지 않는다.
- 작업 후 Codex 주간 잔여89%,5시간 잔여32%; Claude 주간 잔여 UNAVAILABLE.
- GM-34 main 병합과 Jira suspended-inactivity는 여전히 남아 있다.
  코드 작업 완료와 원격 main 병합 완료를 구분한다.
