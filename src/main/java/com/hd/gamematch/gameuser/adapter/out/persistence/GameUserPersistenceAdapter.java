package com.hd.gamematch.gameuser.adapter.out.persistence;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.game.adapter.out.persistence.GamePersistenceMapper;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.application.exception.GameUserAlreadyRegisteredException;
import com.hd.gamematch.gameuser.application.exception.GameUserNicknameAlreadyInUseException;
import com.hd.gamematch.gameuser.application.port.out.ExistsGameUserNicknamePort;
import com.hd.gamematch.gameuser.application.port.out.ExistsGameUserPort;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserByUserAndGamePort;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUserPort;
import com.hd.gamematch.gameuser.application.port.out.LoadGameUsersPort;
import com.hd.gamematch.gameuser.application.port.out.SaveGameUserPort;
import com.hd.gamematch.gameuser.domain.GameUser;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.hibernate.exception.ConstraintViolationException;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class GameUserPersistenceAdapter implements SaveGameUserPort, ExistsGameUserPort, ExistsGameUserNicknamePort,
        LoadGameUserPort, LoadGameUserByUserAndGamePort, LoadGameUsersPort {

    private static final String DUPLICATE_REGISTRATION_CONSTRAINT =
            "uk_game_user_user_id_game_id";
    private static final String DUPLICATE_NICKNAME_CONSTRAINT =
            "uk_game_user_game_id_nickname";

    private final GameUserJpaRepository gameUserJpaRepository;
    private final GameJpaRepository gameJpaRepository;
    private final UserJpaRepository userJpaRepository;

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
            if (hasConstraintViolation(exception, DUPLICATE_REGISTRATION_CONSTRAINT)) {
                throw new GameUserAlreadyRegisteredException(exception);
            }
            if (hasConstraintViolation(exception, DUPLICATE_NICKNAME_CONSTRAINT)) {
                throw new GameUserNicknameAlreadyInUseException();
            }
            throw exception;
        }
    }

    @Override
    public boolean existsByUserIdAndGameId(Long userId, Long gameId) {
        return gameUserJpaRepository.existsByUserIdAndGameId(userId, gameId);
    }

    private boolean hasConstraintViolation(
            DataIntegrityViolationException exception,
            String expectedConstraintName
    ) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolationException) {
                String constraintName = constraintViolationException.getConstraintName();

                // H2는 schema 이름과 내부 index 접미사를 붙일 수 있다.
                return constraintName != null
                        && constraintName.toLowerCase(Locale.ROOT)
                        .contains(expectedConstraintName);
            }

            cause = cause.getCause();
        }

        return false;
    }

    @Override
    public boolean existsByGameIdAndNickname(Long gameId, String nickname) {
        return gameUserJpaRepository.existsByGameIdAndNickname(
                gameId,
                nickname
        );
    }

    @Override
    public java.util.Optional<GameUserProfile> loadGameUserById(Long gameUserId) {
        return gameUserJpaRepository.findById(gameUserId)
                .map(this::toGameUserProfile);
    }

    @Override
    public java.util.Optional<GameUserProfile> loadGameUserByUserIdAndGameId(
            Long userId,
            Long gameId
    ) {
        return gameUserJpaRepository.findByUserIdAndGameId(userId, gameId)
                .map(this::toGameUserProfile);
    }

    @Override
    public List<GameUserProfile> loadGameUsersByNicknamePrefix(
            String nickname,
            Long gameId,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page - 1, size);
        String escapedNickname = escapeLikePattern(nickname);

        List<GameUserJpaEntity> gameUsers = gameUserJpaRepository
                .findByNicknamePrefix(escapedNickname, gameId, pageable);

        return toGameUserProfiles(gameUsers);
    }

    @Override
    public long countGameUsersByNicknamePrefix(String nickname, Long gameId) {
        return gameUserJpaRepository.countByNicknamePrefix(escapeLikePattern(nickname), gameId);
    }

    private String escapeLikePattern(String nickname) {
        return nickname
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private List<GameUserProfile> toGameUserProfiles(List<GameUserJpaEntity> gameUsers) {
        Set<Long> gameIds = gameUsers.stream()
                .map(GameUserJpaEntity::getGameId)
                .collect(Collectors.toSet());
        Set<Long> userIds = gameUsers.stream()
                .map(GameUserJpaEntity::getUserId)
                .collect(Collectors.toSet());

        Map<Long, Game> gamesById = gameJpaRepository.findAllById(gameIds).stream()
                .collect(Collectors.toMap(
                        GameJpaEntity::getId,
                        GamePersistenceMapper::toDomain
                ));
        Set<Long> existingUserIds = userJpaRepository.findAllById(userIds).stream()
                .map(UserJpaEntity::getId)
                .collect(Collectors.toSet());

        // findAllById()의 반환 순서에 의존하지 않고, 검색 쿼리가 만든 페이지 순서를 유지한다.
        return gameUsers.stream()
                .map(gameUser -> toGameUserProfile(gameUser, gamesById, existingUserIds))
                .toList();
    }

    private GameUserProfile toGameUserProfile(GameUserJpaEntity gameUser) {
        // 프로필 행은 존재하지만 연결된 게임·사용자가 없다면 데이터 정합성 오류다.
        // 업무상 "프로필 없음"(404)으로 숨기지 않고 서버 오류로 드러낸다.
        var game = gameJpaRepository.findById(gameUser.getGameId())
                .map(GamePersistenceMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("게임 프로필의 연결 게임이 존재하지 않습니다."));

        if (!userJpaRepository.existsById(gameUser.getUserId())) {
            throw new IllegalStateException("게임 프로필의 연결 사용자가 존재하지 않습니다.");
        }

        return new GameUserProfile(
                gameUser.getId(),
                gameUser.getNickname(),
                game,
                gameUser.getUserId()
        );
    }

    private GameUserProfile toGameUserProfile(
            GameUserJpaEntity gameUser,
            Map<Long, Game> gamesById,
            Set<Long> existingUserIds
    ) {
        Game game = gamesById.get(gameUser.getGameId());
        if (game == null) {
            throw new IllegalStateException("게임 프로필의 연결 게임이 존재하지 않습니다.");
        }

        if (!existingUserIds.contains(gameUser.getUserId())) {
            throw new IllegalStateException("게임 프로필의 연결 사용자가 존재하지 않습니다.");
        }

        return new GameUserProfile(
                gameUser.getId(),
                gameUser.getNickname(),
                game,
                gameUser.getUserId()
        );
    }
}
