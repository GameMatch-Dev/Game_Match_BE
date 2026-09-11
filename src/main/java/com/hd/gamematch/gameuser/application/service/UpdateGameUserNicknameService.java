package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
import com.hd.gamematch.gameuser.application.exception.GameUserAccessDeniedException;
import com.hd.gamematch.gameuser.application.port.in.update.UpdateGameUserNicknameCommand;
import com.hd.gamematch.gameuser.application.port.in.update.UpdateGameUserNicknameUseCase;
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

    @Override
    public GameUserProfile updateNickname(UpdateGameUserNicknameCommand command) {
        GameUserProfile profile = loadGameUserPort.loadGameUserById(command.gameUserId())
                .orElseThrow(GameUserNotFoundException::new);

        if (!profile.userId().equals(command.userId())) {
            throw new GameUserAccessDeniedException();
        }

        // 기존 닉네임과 같으면 성공으로 처리하되, 불필요한 DB UPDATE는 실행하지 않는다.
        if (profile.nickname().equals(command.nickname())) {
            return profile;
        }

        updateGameUserNicknamePort.updateNickname(profile.id(), command.nickname());

        return new GameUserProfile(
                profile.id(),
                command.nickname(),
                profile.game(),
                profile.userId()
        );
    }
}
