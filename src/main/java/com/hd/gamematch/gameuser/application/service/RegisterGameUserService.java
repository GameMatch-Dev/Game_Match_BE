package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.exception.GameUserAlreadyRegisteredException;
import com.hd.gamematch.gameuser.application.exception.GameUserNicknameAlreadyInUseException;
import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserCommand;
import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserUseCase;
import com.hd.gamematch.gameuser.application.port.out.ExistsGameUserNicknamePort;
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

    private final ExistsGameUserNicknamePort existsGameUserNicknamePort;

    @Override
    public Long register(RegisterGameUserCommand command) {

        // 검사
        validateNotAlreadyRegistered(command);
        validateNicknameIsAvailable(command);


        // 실제 register 시작
        GameUser gameUser = GameUser.create(
                command.userId(),
                command.gameId(),
                command.nickname()
        );

        return saveGameUserPort.save(gameUser);
    }

    private void validateNotAlreadyRegistered(
            RegisterGameUserCommand command
    ) {
        if (existsGameUserPort.existsByUserIdAndGameId(
                command.userId(),
                command.gameId()
        )) {
            throw new GameUserAlreadyRegisteredException();
        }
    }

    private void validateNicknameIsAvailable(
            RegisterGameUserCommand command
    ) {
        if (existsGameUserNicknamePort.existsByGameIdAndNickname(
                command.gameId(),
                command.nickname()
        )) {
            throw new GameUserNicknameAlreadyInUseException();
        }
    }
}