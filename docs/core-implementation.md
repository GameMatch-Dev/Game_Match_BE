# 핵심 구현

[README](../README.md) | [API](api.md) | [아키텍처](architecture.md) | [핵심 구현](core-implementation.md) | [로컬 실행](local-setup.md)

[게임 프로필 검색](#게임-프로필-검색에서-반복-조회-줄이기) | [친선경기 목록](#친선경기-목록에서-페이지-대상만-집계하기) | [동시 승인](#동시-승인에서-정원과-참가-기록-지키기)

## 게임 프로필 검색에서 반복 조회 줄이기

변경 전에는 목록을 한 번 조회하고 각 프로필마다 게임 정보와 사용자 존재 여부를 확인했습니다. 변경 후에는 목록에서 ID를 모아 게임과 사용자를 각각 한 번씩 조회합니다. 연결 정보가 누락되면 기존 오류 의미를 유지하며, 결과는 원래 목록 순서대로 조립합니다. [GM-33 설계와 테스트 기록](https://github.com/GameMatch-Dev/Game_Match_BE/blob/main/docs/adr/GM-33-game-user-search-query-strategy.md)

`GameUserPersistenceAdapter.toGameUserProfiles()`에서 ID를 `Set`으로 모아 중복을 제거합니다. 게임은 ID로 찾을 수 있게 `Map`으로 변환하고, 사용자는 연결 여부를 확인할 수 있게 ID만 `Set`에 담습니다.

```java
// toGameUserProfiles()의 조회와 응답 조립 부분
Map<Long, Game> gamesById = gameJpaRepository.findAllById(gameIds).stream()
        .collect(Collectors.toMap(GameJpaEntity::getId, GamePersistenceMapper::toDomain));
Set<Long> existingUserIds = userJpaRepository.findAllById(userIds).stream()
        .map(UserJpaEntity::getId)
        .collect(Collectors.toSet());

return gameUsers.stream()
        .map(gameUser -> toGameUserProfile(gameUser, gamesById, existingUserIds))
        .toList();
```

검색 결과를 다시 순회하는 이유는 DB의 일괄 조회가 입력 ID 순서를 보장하지 않기 때문입니다. 기존 스칼라 ID 구조를 유지하면서 검색 경로만 바꿀 수 있어, 조회 한 건을 줄이기 위해 JPA 연관관계를 새로 추가하는 것보다 변경 범위가 작았습니다. [영속성 어댑터](https://github.com/GameMatch-Dev/Game_Match_BE/blob/main/src/main/java/com/hd/gamematch/gameuser/adapter/out/persistence/GameUserPersistenceAdapter.java)

**검증 결과**

서로 다른 게임과 사용자가 연결된 프로필 2건을 대상으로 Hibernate Statistics에서 SQL 실행 횟수를 확인했습니다. 목록 조회는 **5회에서 최대 3회**로 줄었습니다. 페이지 전체 건수를 구하는 쿼리는 이 측정과 별도입니다.

## 친선경기 목록에서 페이지 대상만 집계하기

경기 목록 실험은 연관 조회 2건과 신청 집계 4건이 경기마다 반복되는 구조를 재현했습니다. EntityGraph에 해당하는 조회 형태로 앞의 2건을 합친 뒤, CTE에서는 표시할 경기를 먼저 고르고 그 경기들의 신청만 일괄 집계했습니다. 수치는 별도 SQL 실험의 실행 횟수입니다.

별도 실험의 `FriendlyMatchBenchmarkService`에서는 다음 순서로 조회합니다.

1. `page_matches`에서 게임 ID로 필터링하고 경기 날짜, 시간, ID 순으로 정렬한 뒤 페이지 대상을 확정합니다.
2. `request_stats`에서 대상 경기의 신청만 조인해 유효, 대기, 승인 건수와 최근 변경 시각을 경기별로 집계합니다.
3. 페이지 대상에 주최자, 게임, 집계 결과를 연결합니다. 신청이 없는 경기도 보존하기 위해 집계 결과에는 `LEFT JOIN`을 사용하고 건수는 `COALESCE`로 0을 채웁니다.

신청 원본 행을 먼저 붙이면 경기 한 건이 여러 행으로 늘어납니다. 페이지를 경기 단위로 먼저 확정하면 그 뒤에 신청 집계를 붙여도 표시할 경기 수가 달라지지 않습니다.

**검증 결과**

경기 20건을 대상으로 별도 SQL 재현 실험을 진행했습니다.

| 조회 방식 | SQL 실행 횟수 |
| --- | ---: |
| 경기마다 연관 정보와 신청 현황을 반복 조회 | 121회 |
| 주최자와 게임 조회를 목록 쿼리에 결합 | 81회 |
| CTE에서 페이지를 확정하고 신청 현황을 일괄 집계 | 1회 |

## 동시 승인에서 정원과 참가 기록 지키기

### 마지막 자리에 승인 요청이 겹치는 경우

정원이 한 자리 남았을 때 서로 다른 참가 신청에 대한 승인 요청이 동시에 들어오면, 두 요청 모두 남은 자리가 있다고 판단할 수 있습니다. 같은 신청을 여러 번 승인할 때는 참가 기록이 중복될 수 있습니다. 두 경우를 막기 위해 정원 확인부터 신청 상태 변경과 참가 기록 생성까지를 하나의 승인 흐름으로 다뤘습니다.

### 비관적 락으로 승인 처리 순서 정하기

비관적 락을 적용한 비교 시험의 처리 흐름입니다.

1. 승인 트랜잭션에서 대상 경기 행에 쓰기 잠금을 획득합니다. 같은 경기에 대한 다음 승인 요청은 앞선 트랜잭션이 끝날 때까지 기다립니다.
2. 잠금을 획득한 뒤 신청 상태와 남은 정원을 확인합니다. 이미 승인한 신청에는 참가 기록을 다시 만들지 않고, 정원이 찬 경기에는 추가 승인을 허용하지 않습니다.
3. 승인 가능한 신청은 상태를 변경하고 참가 기록을 생성한 뒤 같은 트랜잭션에서 커밋합니다. 중간에 실패하면 둘 다 롤백해 신청 상태만 승인으로 남는 상황을 피합니다.

예를 들어 마지막 한 자리를 첫 번째 요청이 채우면, 두 번째 요청은 잠금을 얻은 뒤 정원이 찬 상태를 보고 승인을 중단합니다.

### 선택 이유

낙관적 락에서는 자리가 충분해도 같은 버전의 경기 정보를 읽은 요청 중 하나가 충돌로 실패할 수 있습니다. 그 요청을 계속 처리하려면 최신 상태를 다시 읽고 승인 조건을 확인해야 합니다. 승인 트랜잭션이 짧다는 조건에서, 같은 경기의 요청이 잠깐 기다린 뒤 최신 정원으로 판단하도록 비관적 락을 선택했습니다. 같은 경기에 요청이 집중되거나 트랜잭션이 길어지면 대기 시간이 늘어나는 비용이 있습니다.

**검증 결과**

별도 HTTP 비교 시험에서 승인 요청을 동시에 보내고 처리 후 참가 기록을 확인했습니다. **정원 초과 0건, 중복 참가 0건**이었습니다. 위 처리 흐름과 결과는 비교 시험 환경 기준입니다.
