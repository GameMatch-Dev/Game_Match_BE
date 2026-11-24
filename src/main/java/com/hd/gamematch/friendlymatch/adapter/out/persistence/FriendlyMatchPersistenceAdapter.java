package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.friendlymatch.application.port.out.SaveFriendlyMatchPort;
import com.hd.gamematch.friendlymatch.application.port.out.LoadFriendlyMatchPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveHostRosterPort;
import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.game.adapter.out.persistence.GamePersistenceMapper;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaEntity;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaRepository;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FriendlyMatchPersistenceAdapter implements SaveFriendlyMatchPort,
        LoadFriendlyMatchPort, SaveHostRosterPort {

    private final FriendlyMatchJpaRepository friendlyMatchJpaRepository;
    private final GameUserJpaRepository gameUserJpaRepository;
    private final GameJpaRepository gameJpaRepository;
    private final UserJpaRepository userJpaRepository;

    // 경기 행과 실제 호스트 프로필 및 게임 정보를 한 트랜잭션 안에서 복원한다.
    @Override
    @Transactional(readOnly = true)
    public Optional<FriendlyMatch> loadFriendlyMatch(Long matchId) {
        return friendlyMatchJpaRepository.findById(matchId).map(this::toDomain);
    }

    // 경기 복원으로 연결 데이터를 검증한 뒤 관리 중인 명단을 교체한다.
    @Override
    @Transactional
    public FriendlyMatch saveHostRoster(Long matchId, List<Long> teamMemberIds) {
        FriendlyMatchJpaEntity entity = friendlyMatchJpaRepository.findById(matchId)
                .orElseThrow(FriendlyMatchNotFoundException::new);
        FriendlyMatch result = toDomain(entity);
        entity.confirmHostRoster(teamMemberIds);
        return result;
    }

    // 호스트의 실제 사용자 ID와 프로필에 연결된 게임을 유지하며 경기 전체를 복원한다.
    private FriendlyMatch toDomain(FriendlyMatchJpaEntity entity) {
        GameUserJpaEntity profile = gameUserJpaRepository.findById(entity.getHostGameUserId())
                .orElseThrow(() -> new IllegalStateException("친선 경기의 호스트 프로필이 존재하지 않습니다."));
        Game game = loadGame(entity.getGameId());
        Game hostGame = entity.getGameId().equals(profile.getGameId())
                ? game : loadGame(profile.getGameId());
        // 프로필에 연결된 실제 사용자가 없으면 호스트 도메인을 복원하지 않는다.
        if (!userJpaRepository.existsById(profile.getUserId())) {
            throw new IllegalStateException("게임 프로필의 연결 사용자가 존재하지 않습니다.");
        }
        GameUserProfile host = new GameUserProfile(
                profile.getId(), profile.getNickname(), hostGame, profile.getUserId());
        return new FriendlyMatch(entity.getId(), host, game, entity.getDate(), entity.getTime(),
                entity.getSort(), entity.getState(), entity.getCapacity(), entity.getComment());
    }

    // 연결된 게임이 없는 저장 데이터는 조회 실패로 숨기지 않고 정합성 오류로 드러낸다.
    private Game loadGame(Long gameId) {
        return gameJpaRepository.findById(gameId)
                .map(GamePersistenceMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("친선 경기의 연결 게임이 존재하지 않습니다."));
    }

    // 새 친선 경기 모집을 JPA Entity로 저장하고 DB ID가 담긴 도메인 결과를 반환한다.
    @Override
    public FriendlyMatch save(FriendlyMatch friendlyMatch) {
        // 도메인 객체를 DB 테이블 구조에 맞는 JPA Entity로 변환해 저장한다.
        FriendlyMatchJpaEntity savedEntity = friendlyMatchJpaRepository.save(
                FriendlyMatchJpaEntity.from(friendlyMatch)
        );

        // 호스트와 게임은 생성 요청에서 이미 검증한 도메인 정보를 유지하고, 저장된 컬럼값과 ID를 반영한다.
        return new FriendlyMatch(
                savedEntity.getId(),
                friendlyMatch.host(),
                friendlyMatch.game(),
                savedEntity.getDate(),
                savedEntity.getTime(),
                savedEntity.getSort(),
                savedEntity.getState(),
                savedEntity.getCapacity(),
                savedEntity.getComment()
        );
    }
}
