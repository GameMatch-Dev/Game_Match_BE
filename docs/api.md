# GameMatch API

[README](../README.md) | [API](api.md) | [아키텍처](architecture.md) | [핵심 구현](core-implementation.md) | [로컬 실행](local-setup.md)

아래 표는 이 저장소의 컨트롤러에서 확인한 인증, 게임, 게임 프로필 API입니다. 요청 필드와 응답 DTO는 링크된 소스에서 확인할 수 있습니다.

## 인증

| 기능 | 메서드와 경로 | 동작 |
| --- | --- | --- |
| 카카오 로그인 시작 | `GET /auth/kakao/authorize` | 카카오 인증 요청을 시작합니다. |
| 토큰 교환 | `POST /auth/token` | 로그인 후 토큰을 교환합니다. |
| 내 계정 | `GET /auth/me` | 인증된 사용자 정보를 조회합니다. |

## 게임 조회

| 기능 | 메서드와 경로 | 동작 |
| --- | --- | --- |
| 게임 목록 | `GET /games` | 등록된 게임을 조회합니다. |
| 게임 상세 | `GET /games/{gameId}` | 지정한 게임 정보를 조회합니다. |

## 게임 프로필

| 기능 | 메서드와 경로 | 동작 |
| --- | --- | --- |
| 프로필 생성 | `POST /game-users` | 게임별 사용자 프로필을 만듭니다. |
| 프로필 상세 | `GET /game-users/{gameUserId}` | 게임 프로필 하나를 조회합니다. |
| 내 게임 프로필 | `GET /game-users/me/games/{gameId}` | 해당 게임에 등록한 내 프로필을 조회합니다. |
| 프로필 수정 | `PATCH /game-users/{gameUserId}` | 지정한 게임 프로필을 수정합니다. |
| 프로필 검색 | `GET /game-users/search` | 게임 프로필을 페이지 단위로 검색합니다. |

소스에서 확인하기. [인증](https://github.com/GameMatch-Dev/Game_Match_BE/blob/main/src/main/java/com/hd/gamematch/auth/adapter/in/web/AuthController.java) | [게임](https://github.com/GameMatch-Dev/Game_Match_BE/blob/main/src/main/java/com/hd/gamematch/game/adapter/in/web/GameController.java) | [게임 프로필](https://github.com/GameMatch-Dev/Game_Match_BE/blob/main/src/main/java/com/hd/gamematch/gameuser/adapter/in/web/GameUserController.java)
