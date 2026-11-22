# JWT 인증을 Spring Resource Server로 전환

2026-11-23. 사용자가 커스텀 필터의 필요성을 검토하고 기본 기능으로 전환하도록 승인했다.

## 결정과 범위

기존 JwtAuthenticationFilter의 Bearer 헤더 추출, JwtDecoder 호출, 인증 객체 생성,
SecurityContext 저장은 Spring Resource Server가 제공한다. 별도 필터를 유지해야 할
추가 업무 규칙이 없어 기본 JWT 인증으로 교체한다. 의존성은 이미 포함돼 있다.

기존 HS256 키 생성과 issuer 및 시간 검증을 유지한다. JwtDecoder는 Bean으로
등록해 재사용한다. sub가 Long으로 변환 가능한지 추가 검증해 Controller에서
숫자 변환 중 서버 오류가 발생하지 않도록 한다. Controller는 기본 Jwt principal을
Long으로 캐스팅하지 않고 Authentication.getName()에서 sub를 읽는다.

토큰 발급과 카카오 로그인 흐름은 유지한다. WebMvc 지원 클래스는 JwtDecoder만
Mock으로 대체하며 실제 Spring Bearer 인증 필터를 통과한다.

## 동작 차이

공개 경로도 잘못된 Bearer 토큰이 있으면 401 AUTH_401로 거부한다.
기존 필터는 인증 실패 후 체인을 계속 실행해 공개 경로 접근을 허용했다.
이는 Resource Server의 표준 동작이며 통합 테스트에 명시한다.

## 검증

JwtResourceServerIntegrationTest에서 실제 서명 토큰의 사용자 ID 전달, 토큰 누락,
잘못된 토큰, 서명 변조, 공개 경로의 무토큰 접근, 공개 경로의 잘못된 토큰,
숫자가 아닌 subject를 검사한다. 다른 도메인 회귀는 전체 Gradle test로 확인한다.

## 리뷰 상태

구현 전 사용량 확인: Codex 주간 잔여 45%, Claude UNAVAILABLE.
weekly-ai-usage-budget-guard.md에 따라 추가 에이전트와 Claude 호출은 보류했다.
이후 사용자가 이번 JWT 전환 리뷰에 대한 예외 호출을 명시적으로 승인했다.
호출 전 Codex 주간 잔여 44%, Claude UNAVAILABLE. Codex 독립 리뷰는 결함 없음으로
종료했고 테스트 담당의 P2 보강 제안(만료, issuer 불일치)을 ACCEPT했다.
해당 테스트를 추가한 뒤 JWT 통합 테스트 9개가 통과했고 독립 재검토에서도 추가 지적이 없었다.
제품 코드 변경 후 전체 테스트는 160개 통과(exit 0, 37초), 이후 테스트만 추가한
대상 실행은 9개 통과(exit 0, 20초)로 서로 구분해 기록한다.
Claude 첫 호출은 findings JSON을 반환하지 않아 완료로 인정하지 않았다.
재시도는 findings: []를 반환했다. Claude에 전달한 제품 코드는 최종 제품 코드와 같고,
테스트 증빙은 보강 전 전체 160개 통과와 기존 JWT 7개 케이스 기준이다.
이후 추가한 테스트 2개는 Codex 테스트 담당이 실행하고 독립 리뷰 담당이 재검토했다.
Claude 단독 이슈는 없고 ACCEPT/REJECT할 Claude finding도 없다.
배치 후 사용량 확인: Codex 주간 잔여 42%, Claude UNAVAILABLE.

공식 근거: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
