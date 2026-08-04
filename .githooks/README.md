# Git hooks

`prepare-commit-msg` hook은 브랜치 이름의 Jira 키를 커밋 제목에 자동으로 넣습니다.

- 브랜치: `feature/GM-11-invalid-game-id-400`
- 입력: `feat: 400 응답 처리`
- 결과: `feat(GM-11): 400 응답 처리`

이 저장소를 새로 clone한 뒤에는 한 번만 아래 명령을 실행합니다.

```bash
git config core.hooksPath .githooks
```

Jira 키가 없는 브랜치와 이미 Jira 키가 포함된 커밋 메시지는 변경하지 않습니다.
