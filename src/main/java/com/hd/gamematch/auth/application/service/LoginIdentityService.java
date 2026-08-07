package com.hd.gamematch.auth.application.service;

import com.hd.gamematch.auth.adapter.out.persistence.LoginIdentityJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.LoginIdentityJpaRepository;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaEntity;
import com.hd.gamematch.auth.adapter.out.persistence.UserJpaRepository;
import com.hd.gamematch.auth.domain.LoginProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginIdentityService {

    private final UserJpaRepository userJpaRepository;
    private final LoginIdentityJpaRepository loginIdentityJpaRepository;

    /**
     * 같은 카카오 회원번호는 항상 같은 내부 userId로 귀결된다.
     * DB의 UNIQUE 제약이 동시에 들어온 첫 로그인에서도 중복 연결을 최종 방어한다.
     */
    @Transactional
    public Long findOrCreateKakaoUser(String kakaoMemberId) {
        return loginIdentityJpaRepository
                .findByProviderAndProviderSubject(LoginProvider.KAKAO, kakaoMemberId)
                .map(identity -> identity.getUser().getId())
                .orElseGet(() -> createKakaoUser(kakaoMemberId));
    }

    private Long createKakaoUser(String kakaoMemberId) {
        UserJpaEntity user = userJpaRepository.save(UserJpaEntity.create());
        loginIdentityJpaRepository.save(LoginIdentityJpaEntity.of(user, LoginProvider.KAKAO, kakaoMemberId));
        return user.getId();
    }
}
