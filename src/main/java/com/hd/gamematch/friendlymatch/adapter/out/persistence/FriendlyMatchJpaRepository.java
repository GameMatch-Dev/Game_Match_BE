package com.hd.gamematch.friendlymatch.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

// FriendlyMatchJpaEntity의 기본 저장과 단건 조회를 Spring Data JPA에 위임한다.
public interface FriendlyMatchJpaRepository extends JpaRepository<FriendlyMatchJpaEntity, Long> {
}
