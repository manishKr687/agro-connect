package com.agroconnect.security;

import com.agroconnect.model.BlacklistedToken;
import com.agroconnect.model.RevokedUser;
import com.agroconnect.repository.BlacklistedTokenRepository;
import com.agroconnect.repository.RevokedUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Persistent token revocation store backed by the database.
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final RevokedUserRepository revokedUserRepository;

    public void blacklistToken(String token, Instant expiry) {
        blacklistedTokenRepository.save(BlacklistedToken.builder()
                .token(token)
                .expiresAt(expiry)
                .build());
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokenRepository.existsById(token);
    }

    public void revokeUser(String username) {
        revokedUserRepository.save(RevokedUser.builder()
                .username(username)
                .revokedAt(Instant.now())
                .build());
    }

    public boolean isUserRevoked(String username, Instant tokenIssuedAt) {
        return revokedUserRepository.findById(username)
                .map(RevokedUser::getRevokedAt)
                .filter(tokenIssuedAt::isBefore)
                .isPresent();
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        blacklistedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
