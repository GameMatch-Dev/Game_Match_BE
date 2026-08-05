# Jira·GitHub 작업 흐름

이 문서는 GameMatch Backend의 작업 이력 관리 기준이다. 사람과 Codex는 Jira·GitHub 관련 작업 전에 이 문서를 읽고 같은 순서를 따른다.

## 역할과 기준

- **Jira**는 작업 단위, 요구사항, 상태를 관리하는 단일 기준이다.
- **GitHub**는 코드, 브랜치, 커밋, PR, CI 이력을 관리한다.
- Jira 작업이 있는 범위에는 GitHub Issue를 별도로 만들지 않는다.
- GitHub의 개발 활동은 Jira 키를 통해 해당 Jira 작업의 `개발` 영역에 연결된다.
- 코드 병합과 브랜치 삭제는 자동화 대상이 아니며, 항상 사람의 명시적 승인이 필요하다.

## 표준 작업 순서

1. Jira에서 작업을 만들고 키를 확인한다. 예: `GM-12`.
2. 최신 `main`에서 키를 포함한 작업 브랜치를 만든다. 예: `feature/GM-12-example` 또는 문서 작업은 `docs/GM-12-example`.
3. 작업하고 필요한 테스트·리뷰를 마친다.
4. 변경 파일만 명시적으로 stage하고 커밋한다.
5. 원격 브랜치에 push하고 PR을 만든다. base는 `main`이다.
6. GitHub Actions, 코드리뷰, PR 본문을 확인한다.
7. 사람이 승인하면 `main`에 병합한다.
8. 병합 후 GitHub의 원격 feature 브랜치를 삭제하고 로컬도 정리한다.

## Jira 키와 이름 규칙

Jira 공식 GitHub 연동은 브랜치명·커밋 메시지·PR 제목 또는 본문에서 키를 인식한다. 브랜치명에 키를 넣는 것을 필수 규칙으로 삼는다.

```text
브랜치: feature/GM-12-jira-github-workflow
문서 브랜치: docs/GM-12-jira-github-workflow
커밋: feat(GM-12): 유효하지 않은 게임 ID 400 응답 처리
PR 제목: GM-12: GitHub·Jira 협업 워크플로 문서화
```

커밋 메시지는 Conventional Commits를 기본으로 한다.

```text
feat: 기능 추가
fix: 버그 수정
test: 테스트 추가·수정
docs: 문서 변경
chore: 설정·도구 변경
refactor: 동작을 바꾸지 않는 구조 개선
```

## 커밋 키 자동 삽입

데스크톱의 실제 Git 저장소에서는 `.githooks/prepare-commit-msg`가 브랜치명에서 Jira 키를 찾아 커밋 제목에 넣는다.

```bash
git config core.hooksPath .githooks
git commit -m "docs: 협업 흐름 문서화"
# 저장 결과: docs(GM-12): 협업 흐름 문서화
```

- 이 설정은 **컴퓨터/clone마다 한 번씩** 필요하다.
- 현재 브랜치가 원격 tracking branch와 연결돼 있으면 `git push`만 사용한다.
- iPad의 **Spck Editor 단독 Git 기능은 Git hook을 실행하지 않는다.** 이 경우 커밋 메시지에 `GM-12`를 직접 넣는다.
- Spck CLI로 데스크톱의 실제 저장소에 연결한 경우에는 데스크톱 Git hook이 적용된다.

## PR과 코드리뷰

PR은 작성 중이면 Draft로 만들고, 검토 가능한 상태에서 Ready for review로 전환한다. PR 본문에는 Jira 키, 변경 이유, 검증 결과를 작성한다.

코드리뷰는 다음 안전망을 함께 사용한다.

1. **자동 검사**: GitHub Actions `Backend CI`가 `./gradlew test`를 실행한다.
2. **테스트 담당**: 구현 담당과 분리된 에이전트가 정상·실패·경계 조건을 검증한다.
3. **독립 리뷰 담당**: 구현 담당과 분리된 에이전트가 API 계약, 회귀 위험, 예외 흐름, 유지보수성을 검토한다.
4. **사람의 최종 확인**: PR `Files changed`와 체크를 확인하고 병합 여부를 승인한다.

혼자 개발할 때도 자기 자신에게 GitHub Approve를 남기는 것보다 위 검토 기록과 최종 확인을 남기는 것을 우선한다. 팀 작업에서는 다른 팀원이 `Approve` 또는 `Request changes`를 남긴다.

## Jira 상태 자동화

| GitHub 개발 활동 | Jira 상태 |
| --- | --- |
| Jira 키가 포함된 브랜치 생성 | `할 일 → 진행 중` |
| 연결된 PR 생성 | `진행 중 → 검토 중` |
| 병합하지 않고 PR 닫기 | `검토 중 → 진행 중` |
| 연결된 PR을 `main`에 병합 | `검토 중 → 완료` |

- WIP 커밋을 원격 브랜치에 push하는 것만으로는 작업이 완료되지 않는다.
- PR 제목에 Jira 키를 다시 쓰는 것은 권장하지만 필수는 아니다. 브랜치명에 키가 있으면 공식 연동이 개발 이력을 연결한다.
- 키가 없는 브랜치·커밋·PR은 Jira 개발 이력에 연결되지 않을 수 있다.

## 병합과 브랜치 정리

병합하기 전에는 다음을 확인한다.

- GitHub Actions가 통과했다.
- PR이 Draft가 아니다.
- 테스트·독립 리뷰 결과에 blocker가 없다.
- 사람이 `main` 병합을 명시적으로 승인했다.

병합 후에는 feature 브랜치를 삭제한다. 이미 `main`에 들어간 커밋, PR 기록, Jira 개발 이력은 삭제되지 않는다.

```bash
git switch main
git pull --ff-only
git branch -d feature/GM-12-example
git fetch --prune
```

`git branch -d`는 병합되지 않은 로컬 브랜치를 삭제하지 않는 안전한 명령이다. GitHub에서 원격 브랜치를 삭제한 뒤 `git fetch --prune`으로 오래된 원격 참조를 정리한다.

## Codex 작업 규칙

- GitHub·Jira 변경 전 이 문서와 `AGENTS.md`를 확인한다.
- 코드 변경은 Backend human-led 흐름과 학습 모드 규칙을 따른다.
- 문서·자동화 작업도 Jira 작업과 PR로 추적한다.
- 명시적 승인 없이는 PR 병합, 브랜치 삭제, 운영 데이터 변경을 하지 않는다.
- 작업 결과에는 Jira 키, 브랜치, 검증 결과, 남은 위험을 간단히 남긴다.
