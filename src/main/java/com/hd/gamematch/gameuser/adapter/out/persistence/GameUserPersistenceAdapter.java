package com.hd.gamematch.gameuser.adapter.out.persistence;

import com.hd.gamematch.gameuser.application.exception.GameUserAlreadyRegisteredException;
import com.hd.gamematch.gameuser.application.port.out.ExistsGameUserPort;
import com.hd.gamematch.gameuser.application.port.out.SaveGameUserPort;
import com.hd.gamematch.gameuser.domain.GameUser;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;


@Component
@RequiredArgsConstructor
public class GameUserPersistenceAdapter implements SaveGameUserPort, ExistsGameUserPort {

    private final GameUserJpaRepository gameUserJpaRepository;

    @Override
    public Long save(GameUser gameUser){
        GameUserJpaEntity gameUserJpaEntity = GameUserJpaEntity.of(
                gameUser.getUserId(),
                gameUser.getGameId(),
                gameUser.getNickname()
        );

        try {
            // 현재 등록 흐름은 한 건만 저장하며, 저장 직후 강제 flush가 필요한 요구사항이 없다.
            // 불필요하게 영속성 컨텍스트 전체를 동기화하지 않기 위해 saveAndFlush() 대신 save()를 사용한다.
            return gameUserJpaRepository.save(gameUserJpaEntity).getId();
        } catch (DataIntegrityViolationException exception) {
            // 등록 중복을 보장하는 유니크 제약 위반만 업무 예외로 바꾼다.
            if (isDuplicateRegistrationConstraintViolation(exception)) {
                throw new GameUserAlreadyRegisteredException(exception);
            }
            throw exception;
        }
    }

    @Override
    public boolean existsByUserIdAndGameId(Long userId, Long gameId) {
        return gameUserJpaRepository.existsByUserIdAndGameId(userId, gameId);
    }

    private boolean isDuplicateRegistrationConstraintViolation(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException) {
                String constraintName = constraintViolationException.getConstraintName();

                // H2는 schema 이름과 내부 index 접미사를 붙일 수 있다.
                return constraintName != null
                        && constraintName.toLowerCase(Locale.ROOT)
                        .contains("uk_game_user_user_id_game_id");
            }

            cause = cause.getCause();
        }

        return false;
    }
}
