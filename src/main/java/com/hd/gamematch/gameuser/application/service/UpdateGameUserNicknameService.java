package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.exception.GameUserNotFoundException;
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

        updateGameUserNicknamePort.updateNickname(profile.id(), command.nickname());

        return new GameUserProfile(
                profile.id(),
                command.nickname(),
                profile.game(),
                profile.userId()
        );
    }
}
