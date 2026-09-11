package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import com.hd.gamematch.friendlymatch.application.port.out.SaveFriendlyMatchPort;
import com.hd.gamematch.friendlymatch.domain.FriendlyMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FriendlyMatchPersistenceAdapter implements SaveFriendlyMatchPort {

    private final FriendlyMatchJpaRepository friendlyMatchJpaRepository;

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
                savedEntity.getRecruit(),
                savedEntity.getComment()
        );
    }
}
