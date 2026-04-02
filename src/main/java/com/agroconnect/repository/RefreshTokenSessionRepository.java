package com.agroconnect.repository;

import com.agroconnect.model.RefreshTokenSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenSessionRepository extends JpaRepository<RefreshTokenSession, Long> {
    Optional<RefreshTokenSession> findByTokenHash(String tokenHash);
    void deleteByExpiresAtBefore(Instant instant);
    void deleteByUserPhoneNumber(String phoneNumber);
}
