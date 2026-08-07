package com.hd.gamematch.auth.adapter.out.persistence;

import com.hd.gamematch.auth.domain.LoginProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "login_identity",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_login_identity_provider_subject",
                columnNames = {"provider", "provider_subject"}
        )
)
public class LoginIdentityJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_login_identity_user"))
    private UserJpaEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoginProvider provider;

    // 카카오 회원번호처럼 공급자가 보장하는 고유 식별자다. 카카오 토큰 자체는 저장하지 않는다.
    @Column(name = "provider_subject", nullable = false, length = 100)
    private String providerSubject;

    private LoginIdentityJpaEntity(UserJpaEntity user, LoginProvider provider, String providerSubject) {
        this.user = user;
        this.provider = provider;
        this.providerSubject = providerSubject;
    }

    public static LoginIdentityJpaEntity of(
            UserJpaEntity user,
            LoginProvider provider,
            String providerSubject
    ) {
        return new LoginIdentityJpaEntity(user, provider, providerSubject);
    }
}
