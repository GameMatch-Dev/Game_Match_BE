# 로컬 실행

[README](../README.md) | [API](api.md) | [아키텍처](architecture.md) | [핵심 구현](core-implementation.md) | [로컬 실행](local-setup.md)

## 준비와 실행

JDK 17을 준비한 뒤 백엔드 저장소 루트에서 Gradle Wrapper를 사용합니다.

```sh
./gradlew test
./gradlew bootRun
```

Windows PowerShell에서는 `./gradlew.bat test`와 `./gradlew.bat bootRun`을 실행합니다.

## 환경 변수

MySQL을 연결할 때는 `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`를 실행 환경에 설정합니다. 카카오 로그인까지 확인할 때는 `KAKAO_REST_API_KEY`, `KAKAO_CLIENT_SECRET`, `FRONTEND_LOGIN_REDIRECT_URI`와 JWT 설정을 준비합니다. 프런트엔드 주소가 다르면 `CORS_ALLOWED_ORIGINS`도 맞춥니다. 변수 이름과 예시 값은 저장소의 [`.env.example`](https://github.com/GameMatch-Dev/Game_Match_BE/blob/main/.env.example)을 기준으로 합니다.

프로필별 DB 설정과 스키마 적용 방식은 [DB 마이그레이션 문서](https://github.com/GameMatch-Dev/Game_Match_BE/blob/main/docs/database-migration.md)를 참고합니다.
