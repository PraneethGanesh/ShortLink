package com.praneeth.identityservice.repository;

import com.praneeth.identityservice.entity.RegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RegistrationTokenRepository
        extends JpaRepository<RegistrationToken, Long> {

    Optional<RegistrationToken> findByTokenHash(String tokenHash);

    Optional<RegistrationToken> findTopByEmailOrderByCreatedAtDesc(
            String email
    );
    List<RegistrationToken> findAllByEmailAndUsedFalse(String email);

    void deleteByExpiresAtBefore(Instant time);
}
