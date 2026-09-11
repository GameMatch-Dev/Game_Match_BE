package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchCommand;
import com.hd.gamematch.friendlymatch.application.port.in.create.CreateFriendlyMatchUseCase;
import com.hd.gamematch.friendlymatch.application.port.out.SaveFriendlyMatchPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserQuery;
import com.hd.gamematch.gameuser.application.port.in.find.FindGameUserUseCase;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CreateFriendlyMatchService implements CreateFriendlyMatchUseCase {

    private final FindGameUserUseCase findGameUserUseCase;
    private final SaveFriendlyMatchPort saveFriendlyMatchPort;

    // 호스트 게임 프로필을 기준으로 친선 경기 모집을 만들고 저장한다.
    @Override
    public FriendlyMatch create(CreateFriendlyMatchCommand command) {
        // 호스트 프로필과 연결 게임은 요청값이 아니라 기존 게임 프로필에서 확인한다.
        GameUserProfile host = findGameUserUseCase.findGameUser(
                FindGameUserQuery.of(command.hostGameUserId())
        );

        // JWT 사용자와 호스트 프로필 소유자가 다르면 타인의 프로필로 모집을 만들 수 없다.
        if (!host.userId().equals(command.userId())) {
            throw new IllegalArgumentException("자신의 게임 프로필로만 친선 경기 모집을 만들 수 있습니다.");
        }

        // DB가 ID를 발급하기 전이므로 새 매치는 아직 식별자가 없다.
        FriendlyMatch friendlyMatch = new FriendlyMatch(
                null,
                host,
                host.game(),
                command.date(),
                command.time(),
                command.sort(),
                FriendlyMatchState.RECRUITING,
                command.recruit(),
                command.comment()
        );

        // 저장 Adapter가 ID를 채운 생성 완료 매치를 반환한다.
        return saveFriendlyMatchPort.save(friendlyMatch);
    }
}
