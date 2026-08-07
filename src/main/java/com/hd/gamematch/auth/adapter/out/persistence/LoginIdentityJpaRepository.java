package com.hd.gamematch.auth.adapter.out.persistence;

import com.hd.gamematch.auth.domain.LoginProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoginIdentityJpaRepository extends JpaRepository<LoginIdentityJpaEntity, Long> {

    Optional<LoginIdentityJpaEntity> findByProviderAndProviderSubject(
            LoginProvider provider,
            String providerSubject
    );
}
