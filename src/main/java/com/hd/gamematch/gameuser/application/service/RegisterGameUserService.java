package com.hd.gamematch.gameuser.application.service;

import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserCommand;
import com.hd.gamematch.gameuser.application.port.in.RegisterGameUserUseCase;
import com.hd.gamematch.gameuser.application.port.out.SaveGameUserPort;
import com.hd.gamematch.gameuser.domain.GameUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegisterGameUserService implements RegisterGameUserUseCase {

    private final SaveGameUserPort saveGameUserPort;

    @Override
    public void register(RegisterGameUserCommand command) {
        GameUser gameUser = GameUser.create(
                command.userId(),
                command.gameId(),
                command.nickname()
        );

        saveGameUserPort.save(gameUser);
    }
}