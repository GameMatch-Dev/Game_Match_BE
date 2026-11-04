# GameMatch

게임별 커뮤니티에서 유저를 찾고, 친선경기를 모집하거나 참가를 신청하는 서비스입니다.

![GameMatch 서비스 미리보기](https://raw.githubusercontent.com/Siwon-Choi/Game_Match_FE/main/assets/readme/game_match.gif)

*[프런트엔드 저장소](https://github.com/Siwon-Choi/Game_Match_FE)*

[API](docs/api.md) | [아키텍처](docs/architecture.md) | [핵심 구현](docs/core-implementation.md)

## 주요 기능

| 기능 | 사용자가 할 수 있는 일 |
| --- | --- |
| 계정 | 회원가입하고 로그인하거나 로그아웃합니다. |
| 게임 탐색 | 게임 목록을 탐색하고 게임별 커뮤니티로 이동합니다. |
| 게임별 프로필 | 게임별 프로필을 등록하고 다른 유저의 프로필을 검색합니다. |
| 그룹 | 그룹 단위로 유저를 모아 함께 활동합니다. |
| 친선경기 | 경기를 찾거나 모집하고, 참가를 신청한 뒤 신청 상태를 확인합니다. 주최자는 신청을 승인하거나 거절합니다. |
| 게시판 | 게임별 게시글 목록과 상세 내용을 보고 새 글을 작성합니다. |

**대표 이용 흐름**은 경기 찾기 → 참가 신청 → 주최자 승인 → 참가 확정입니다.

## 기술 스택

![Java 17](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4E8528?style=flat-square&logo=springboot&logoColor=white) ![Spring Security](https://img.shields.io/badge/Spring%20Security-4E8528?style=flat-square&logo=springsecurity&logoColor=white) ![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-4E8528?style=flat-square&logo=spring&logoColor=white)

![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white) ![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=flat-square&logo=flyway&logoColor=white) ![Gradle](https://img.shields.io/badge/Gradle-02303A?style=flat-square&logo=gradle&logoColor=white)

[로컬 실행 안내](docs/local-setup.md)
