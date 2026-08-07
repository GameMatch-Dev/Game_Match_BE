package com.hd.gamematch.auth.domain;

/**
 * 외부 로그인 공급자는 Auth 영역에서만 안다.
 * 다른 도메인은 공급자 이름 대신 GameMatch 내부 userId만 사용한다.
 */
public enum LoginProvider {
    KAKAO
}
