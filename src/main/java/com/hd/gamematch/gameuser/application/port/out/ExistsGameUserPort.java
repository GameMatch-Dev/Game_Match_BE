package com.hd.gamematch.gameuser.application.port.out;

/**
 * 특정 사용자가 특정 게임의 프로필을 이미 가지고 있는지 확인한다.
 *
 * <p>Service는 DB 기술을 모르고, 이 Port를 통해 중복 여부만 질문한다.</p>
 */
public interface ExistsGameUserPort {

    boolean existsByUserIdAndGameId(Long userId, Long gameId);
}