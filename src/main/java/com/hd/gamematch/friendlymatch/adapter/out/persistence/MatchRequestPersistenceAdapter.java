package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.friendlymatch.application.exception.MatchRequestAlreadyActiveException;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchRequestPort;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchRequestTargetPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveMatchRequestPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchState;
import com.hd.gamematch.friendlymatch.domain.MatchRequest;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import com.hd.gamematch.friendlymatch.domain.MatchRequestTarget;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MatchRequestPersistenceAdapter implements LoadMatchRequestTargetPort,
        LoadMatchRequestPort, SaveMatchRequestPort {

    private static final String DUPLICATE_REQUEST_CONSTRAINT = "uk_match_request_match_id_game_user_id";

    private final FriendlyMatchJpaRepository friendlyMatchJpaRepository;
    private final MatchRequestJpaRepository matchRequestJpaRepository;
    private final EntityManager entityManager;

    // 저장된 경기의 게임과 현재 모집 상태를 조회한다.
    @Override
    @Transactional(readOnly = true)
    public Optional<MatchRequestTarget> loadMatchRequestTarget(Long matchId) {
        return friendlyMatchJpaRepository.findById(matchId)
                .map(match -> new MatchRequestTarget(match.getId(), match.getGameId(),
                        match.getState() == FriendlyMatchState.RECRUITING));
    }

    // 트랜잭션 안에서 신청과 지연 로딩된 팀원 목록을 복원한다.
    @Override
    @Transactional(readOnly = true)
    public Optional<MatchRequest> loadByMatchIdAndGameUserId(Long matchId, Long gameUserId) {
        return matchRequestJpaRepository.findByMatchIdAndGameUserId(matchId, gameUserId)
                .map(MatchRequestJpaEntity::toDomain);
    }

    // 신청 행과 팀원 목록을 한 트랜잭션으로 저장한다.
    @Override
    @Transactional
    public Long save(MatchRequest request) {
        MatchRequestJpaEntity entity;
        if (request.id() == null) {
            entity = MatchRequestJpaEntity.from(request);
        } else {
            // 기존 행을 유지하며 비활성 상태를 선점한 재신청만 허용한다.
            entity = matchRequestJpaRepository.findById(request.id()).orElseThrow();
            int updatedRows = matchRequestJpaRepository.reactivateIfInactive(
                    request.id(), request.matchId(), request.gameUserId(), MatchRequestState.PENDING,
                    request.comment(), List.of(MatchRequestState.CANCELLED, MatchRequestState.REJECTED));
            if (updatedRows == 0) {
                throw new MatchRequestAlreadyActiveException();
            }
            // 사전 조회로 남은 이전 상태가 조건부 갱신 결과를 덮어쓰지 않게 복원한다.
            entityManager.refresh(entity);
            entity.update(request);
        }

        try {
            // Adapter 안에서 SQL을 실행해 유니크 제약 위반을 여기서 변환한다.
            return matchRequestJpaRepository.saveAndFlush(entity).getId();
        } catch (DataIntegrityViolationException exception) {
            if (hasDuplicateRequestConstraint(exception)) {
                throw new MatchRequestAlreadyActiveException();
            }
            throw exception;
        }
    }

    // 신청 조합의 이름 있는 유니크 제약 위반만 판별한다.
    private boolean hasDuplicateRequestConstraint(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException constraintViolation) {
                String constraintName = constraintViolation.getConstraintName();
                // H2가 붙이는 스키마 이름과 내부 인덱스 접미사도 허용한다.
                return constraintName != null && constraintName.toLowerCase(Locale.ROOT)
                        .contains(DUPLICATE_REQUEST_CONSTRAINT);
            }
            cause = cause.getCause();
        }
        return false;
    }
}
