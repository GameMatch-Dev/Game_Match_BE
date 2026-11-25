package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.friendlymatch.application.exception.FriendlyMatchNotFoundException;
import com.hd.gamematch.friendlymatch.application.exception.MatchApprovalConflictException;
import com.hd.gamematch.friendlymatch.application.exception.MatchRequestNotFoundException;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchApprovalContextPort;
import com.hd.gamematch.friendlymatch.application.port.out.LoadMatchRequestForApprovalPort;
import com.hd.gamematch.friendlymatch.application.port.out.SaveMatchRequestApprovalPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatchSort;
import com.hd.gamematch.friendlymatch.domain.MatchApprovalContext;
import com.hd.gamematch.friendlymatch.domain.MatchRequest;
import com.hd.gamematch.friendlymatch.domain.MatchRequestState;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaEntity;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class MatchRequestApprovalPersistenceAdapter implements LoadMatchRequestForApprovalPort,
        LoadMatchApprovalContextPort, SaveMatchRequestApprovalPort {

    private final FriendlyMatchJpaRepository matchRepository;
    private final MatchRequestJpaRepository requestRepository;
    private final MatchParticipantJpaRepository participantRepository;
    private final GameUserJpaRepository profileRepository;
    private final UserJpaRepository userRepository;
    private final GameJpaRepository gameRepository;
    private final EntityManager entityManager;

    // 경기 행을 먼저 잠그기 위해 신청 Entity 대신 경기 식별자만 조회한다.
    @Override
    public Optional<Long> findMatchIdByRequestId(Long requestId) {
        return requestRepository.findMatchIdByRequestId(requestId);
    }

    // 경기 잠금 뒤 신청 행도 잠그고 관리 중인 오래된 상태가 있다면 갱신한다.
    @Override
    public Optional<MatchRequest> loadRequestById(Long requestId) {
        return requestRepository.findByIdForUpdate(requestId).map(entity -> {
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return entity.toDomain();
        });
    }

    // 경기 쓰기 잠금을 유지한 채 실제 주최자와 두 참가 명단을 복원한다.
    @Override
    public Optional<MatchApprovalContext> loadForUpdate(Long matchId) {
        return matchRepository.findByIdForUpdate(matchId).map(match -> {
            entityManager.refresh(match, LockModeType.PESSIMISTIC_WRITE);
            GameUserJpaEntity host = profileRepository.findById(match.getHostGameUserId())
                    .orElseThrow(() -> new IllegalStateException("친선 경기의 호스트 프로필이 존재하지 않습니다."));
            if (!userRepository.existsById(host.getUserId())) {
                throw new IllegalStateException("게임 프로필의 연결 사용자가 존재하지 않습니다.");
            }
            if (!gameRepository.existsById(match.getGameId())
                    || !gameRepository.existsById(host.getGameId())) {
                throw new IllegalStateException("친선 경기의 연결 게임이 존재하지 않습니다.");
            }
            List<Long> approvedIds = participantRepository
                    .findByMatchIdOrderByRequestIdAscMemberOrderAsc(matchId).stream()
                    .map(MatchParticipantJpaEntity::getGameUserId).toList();
            boolean opponentApproved = match.getSort() == FriendlyMatchSort.TEAM
                    && !requestRepository.findByMatchIdAndStateOrderById(matchId, MatchRequestState.APPROVED).isEmpty();
            return new MatchApprovalContext(match.getId(), host.getUserId(), match.getGameId(),
                    match.getSort(), match.getState(), match.getCapacity(), match.isHostRosterConfirmed(),
                    match.getHostTeamMemberIds(), approvedIds, opponentApproved);
        });
    }

    // 호출자의 경기 잠금과 트랜잭션 안에서 참가자와 신청 및 모집 상태를 함께 저장한다.
    @Override
    public void saveApproval(Long matchId, Long requestId, List<Long> participantIds, boolean closeRecruitment) {
        FriendlyMatchJpaEntity match = matchRepository.findByIdForUpdate(matchId)
                .orElseThrow(FriendlyMatchNotFoundException::new);
        MatchRequestJpaEntity request = requestRepository.findByIdForUpdate(requestId)
                .orElseThrow(MatchRequestNotFoundException::new);
        if (!request.getMatchId().equals(matchId)) {
            throw new IllegalArgumentException("신청이 대상 경기에 속하지 않습니다.");
        }
        if (request.getState() != MatchRequestState.PENDING) {
            throw new MatchApprovalConflictException("대기 중인 참가 신청만 승인 저장할 수 있습니다.");
        }
        List<MatchParticipantJpaEntity> participants = new ArrayList<>();
        for (int order = 0; order < participantIds.size(); order++) {
            participants.add(MatchParticipantJpaEntity.of(matchId, requestId, participantIds.get(order), order));
        }
        participantRepository.saveAll(participants);
        request.approve();
        if (closeRecruitment) {
            match.closeRecruitment();
        }
    }
}
