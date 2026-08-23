# 데이터베이스 마이그레이션 기준

GameMatch Backend의 스키마는 Flyway migration으로 관리한다.

## 현재 기준

- MySQL은 **새 빈 데이터베이스**에서 시작한다.
- 애플리케이션을 처음 실행하면 Flyway가 `V1__create_initial_schema.sql`을 적용한다.
- Hibernate는 스키마를 생성하거나 수정하지 않고, 엔티티 매핑만 검증한다 (`ddl-auto: validate`).
- 이후 스키마 변경은 기존 V1을 수정하지 않고 `V2`, `V3`처럼 새 migration 파일을 추가한다.

## 기존 데이터베이스 주의

이미 테이블 또는 데이터가 있는 데이터베이스에는 현재 V1을 그대로 적용하면 안 된다.
그 경우에는 기존 스키마 상태를 확인한 뒤 baseline 및 별도 migration 계획을 먼저 수립한다.
