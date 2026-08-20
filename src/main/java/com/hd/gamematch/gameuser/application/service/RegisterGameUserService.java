package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.exception.GameUserAlreadyRegisteredException;
import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserCommand;
import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserUseCase;
import com.hd.gamematch.gameuser.application.port.out.ExistsGameUserPort;
import com.hd.gamematch.gameuser.application.port.out.SaveGameUserPort;
import com.hd.gamematch.gameuser.domain.GameUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterGameUserService implements RegisterGameUserUseCase {

    private final SaveGameUserPort saveGameUserPort;

    private final ExistsGameUserPort existsGameUserPort;

    @Override
    public Long register(RegisterGameUserCommand command) {

        // 같은 사용자가 같은 게임에 이미 등록했으면 새 프로필을 만들지 않는다.
        if (existsGameUserPort.existsByUserIdAndGameId(
                command.userId(),
                command.gameId()
        )) {
            throw new GameUserAlreadyRegisteredException();
        }

        GameUser gameUser = GameUser.create(
                command.userId(),
                command.gameId(),
                command.nickname()
        );

        return saveGameUserPort.save(gameUser);
    }
}