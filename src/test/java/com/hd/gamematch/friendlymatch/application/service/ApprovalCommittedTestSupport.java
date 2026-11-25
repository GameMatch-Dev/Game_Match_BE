package com.hd.gamematch.friendlymatch.application.service;

import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.security.JwtTokenService;
import com.hd.gamematch.friendlymatch.adapter.out.persistence.*;
import com.hd.gamematch.friendlymatch.domain.*;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaEntity;
import com.hd.gamematch.game.adapter.out.persistence.GameJpaRepository;
import com.hd.gamematch.game.domain.Game;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaEntity;
import com.hd.gamematch.gameuser.adapter.out.persistence.GameUserJpaRepository;
import com.hd.gamematch.gameuser.domain.GameUserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// 테스트 자체에는 트랜잭션을 걸지 않고 준비와 검증을 별도 커밋 경계에서 수행한다.
abstract class ApprovalCommittedTestSupport {
    @Autowired UserJpaRepository users;
    @Autowired GameJpaRepository games;
    @Autowired GameUserJpaRepository profiles;
    @Autowired FriendlyMatchJpaRepository matches;
    @Autowired MatchRequestJpaRepository requests;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JwtTokenService tokens;

    final List<Long> userIds = new ArrayList<>();
    final List<Long> profileIds = new ArrayList<>();
    final List<Long> matchIds = new ArrayList<>();
    Game game;
    GameUserProfile host;
    GameUserProfile hostMember;

    // 다른 스레드가 읽을 수 있도록 준비 데이터를 실제로 커밋한다.
    @BeforeEach
    void prepareCommittedUsers() {
        userIds.clear(); profileIds.clear(); matchIds.clear();
        inNewTransaction(() -> {
            GameJpaEntity saved = games.saveAndFlush(GameJpaEntity.of("approval-test", "test", null));
            game = Game.of(saved.getId(), saved.getName(), saved.getSort(), saved.getUrl());
            host = newProfile();
            hostMember = newProfile();
            return null;
        });
    }

    // 이 테스트가 만든 식별자만 FK의 역순으로 정리하고 삭제도 커밋한다.
    @AfterEach
    void removeCommittedFixtures() {
        inNewTransaction(() -> {
            for (Long id : matchIds) {
                jdbc.update("delete from friendly_match_participant where match_id=?", id);
                jdbc.update("delete from match_request_member where request_id in (select id from match_request where match_id=?)", id);
                jdbc.update("delete from match_request where match_id=?", id);
                jdbc.update("delete from friendly_match_host_member where match_id=?", id);
                jdbc.update("delete from friendly_match where id=?", id);
            }
            for (Long id : profileIds) jdbc.update("delete from game_user where id=?", id);
            for (Long id : userIds) jdbc.update("delete from users where id=?", id);
            if (game != null) jdbc.update("delete from game where id=?", game.id());
            return null;
        });
    }

    // 외부 테스트 트랜잭션과 분리된 새 트랜잭션에서 결과를 읽거나 데이터를 준비한다.
    <T> T inNewTransaction(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.setTimeout(30);
        return template.execute(status -> action.get());
    }

    // 신청 대표와 주최 측 실제 참가자를 구분할 프로필을 만든다.
    GameUserProfile newProfile() {
        Long userId = users.saveAndFlush(UserJpaEntity.create()).getId();
        userIds.add(userId);
        GameUserJpaEntity profile = profiles.saveAndFlush(GameUserJpaEntity.of(
                userId, game.id(), "player" + userId));
        profileIds.add(profile.getId());
        return new GameUserProfile(profile.getId(), profile.getNickname(), game, userId);
    }

    // 주최자는 관리 권한만 가지며 실제 명단에는 별도 주최 참가자 한 명을 넣는다.
    Long newMatch(FriendlyMatchSort sort, int capacity) {
        FriendlyMatchJpaEntity entity = FriendlyMatchJpaEntity.from(new FriendlyMatch(
                null, host, game, LocalDate.of(2027, 6, 10), LocalTime.of(20, 0),
                sort, FriendlyMatchState.RECRUITING, capacity, "독립 트랜잭션 검증"));
        entity.confirmHostRoster(List.of(hostMember.id()));
        Long id = matches.saveAndFlush(entity).getId();
        matchIds.add(id);
        return id;
    }

    // 대표자 한 명이 실제로 참가하는 대기 신청을 저장한다.
    Long newRequest(Long matchId) {
        return requests.saveAndFlush(MatchRequestJpaEntity.from(new MatchRequest(
                null, matchId, newProfile().id(), List.of(), "참가", MatchRequestState.PENDING))).getId();
    }

    // JDBC 조회로 다른 트랜잭션에서 커밋된 실제 행 수를 확인한다.
    int participantCount(Long matchId) {
        return jdbc.queryForObject("select count(*) from friendly_match_participant where match_id=?", Integer.class, matchId);
    }

    // 신청 상태를 JPA 캐시 대신 SQL로 확인한다.
    String requestState(Long requestId) {
        return jdbc.queryForObject("select state from match_request where id=?", String.class, requestId);
    }

    // 경기 상태를 JPA 캐시 대신 SQL로 확인한다.
    String matchState(Long matchId) {
        return jdbc.queryForObject("select state from friendly_match where id=?", String.class, matchId);
    }
}
