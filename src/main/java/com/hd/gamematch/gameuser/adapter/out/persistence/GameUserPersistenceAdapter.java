package com.hd.gamematch.gameuser.adapter.out.persistence;

import com.hd.gamematch.gameuser.application.port.out.SaveGameUserPort;
import com.hd.gamematch.gameuser.domain.GameUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class GameUserPersistenceAdapter implements SaveGameUserPort {

    private final GameUserJpaRepository gameUserJpaRepository;

    @Override
    public Long save(GameUser gameUser){
        GameUserJpaEntity gameUserJpaEntity = GameUserJpaEntity.of(
                gameUser.getUserId(),
                gameUser.getGameId(),
                gameUser.getNickname()
        );

        return gameUserJpaRepository.save(gameUserJpaEntity).getId();
    }
}
