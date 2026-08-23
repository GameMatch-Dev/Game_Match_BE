package com.hd.gamematch.gameuser.application.port.out;

/**
 * 특정 게임에서 닉네임이 이미 사용 중인지 확인한다.
 *
 * <p>Service는 DB 구현을 직접 알지 않고,
 * 이 Port를 통해 닉네임 중복 여부만 확인한다.</p>
 */
public interface ExistsGameUserNicknamePort {

    boolean existsByGameIdAndNickname(Long gameId, String nickname);
}