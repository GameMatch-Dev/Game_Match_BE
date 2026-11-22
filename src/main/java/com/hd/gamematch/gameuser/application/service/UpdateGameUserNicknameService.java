package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.exception.GameUserAccessDeniedException;
import com.hd.gamematch.gameuser.application.exception.GameUserNicknameAlreadyInUseException;
import com.hd.gamematch.gameuser.application.port.in.update.UpdateGameUserNicknameCommand;
import com.hd.gamematch.gameuser.application.port.in.update.UpdateGameUserNicknameUseCase;
import com.hd.gamematch.gameuser.application.port.out.ExistsGameUserNicknamePort;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserPort;
import com.hd.gamematch.gameuser.application.port.out.UpdateGameUserNicknamePort;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateGameUserNicknameService implements UpdateGameUserNicknameUseCase {

    private final LoadGameUserPort loadGameUserPort;
    private final UpdateGameUserNicknamePort updateGameUserNicknamePort;
    private final ExistsGameUserNicknamePort existsGameUserNicknamePort;

    @Override
    public GameUserProfile updateNickname(UpdateGameUserNicknameCommand command) {
        GameUserProfile profile = loadGameUserPort.loadGameUserById(command.gameUserId())
                .orElseThrow(GameUserNotFoundException::new);

        if (!profile.userId().equals(command.userId())) {
            throw new GameUserAccessDeniedException();
        }

        // 자기 자신의 닉네임을 중복으로 오인하지 않고, 불필요한 DB UPDATE도 피한다.
        if (profile.nickname().equals(command.nickname())) {
            return profile;
        }

        // 중복 기준은 (gameId, nickname)이다. gameId는 별도 요청값 대신 조회와 소유권을 확인한
        // 프로필에서 가져오고, nickname은 변경하려는 값이다. 동일 닉네임은 앞에서 반환했으므로
        // 자기 자신을 제외하는 별도 조회 조건은 필요 없다. 동시 요청의 최종 정합성은
        // DB unique 제약과 persistence adapter의 예외 변환이 보장한다.
        if (existsGameUserNicknamePort.existsByGameIdAndNickname(
                profile.game().id(),
                command.nickname()
        )) {
            throw new GameUserNicknameAlreadyInUseException();
        }

        // 사전 중복 검사를 통과한 경우에만 검증된 프로필 ID의 닉네임 변경을 요청한다.
        updateGameUserNicknamePort.updateNickname(profile.id(), command.nickname());

        return new GameUserProfile(
                profile.id(),
                command.nickname(),
                profile.game(),
                profile.userId()
        );
    }
}
