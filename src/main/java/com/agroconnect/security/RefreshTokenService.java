package com.agroconnect.security;

import com.agroconnect.model.RefreshTokenSession;
import com.agroconnect.model.User;
import com.agroconnect.repository.RefreshTokenSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshSession issueToken(User user) {
        String rawToken = generateRawToken();
        Instant now = Instant.now();

        refreshTokenSessionRepository.save(RefreshTokenSession.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .issuedAt(now)
                .expiresAt(now.plusMillis(refreshExpirationMs))
                .build());

        return new RefreshSession(rawToken, now.plusMillis(refreshExpirationMs));
    }

    public RotatedRefreshSession rotate(String rawToken) {
        RefreshTokenSession session = getValidSession(rawToken);
        session.setRevokedAt(Instant.now());
        refreshTokenSessionRepository.save(session);
        RefreshSession nextSession = issueToken(session.getUser());
        return new RotatedRefreshSession(session.getUser(), nextSession.token(), nextSession.expiresAt());
    }

    public void revoke(String rawToken) {
        refreshTokenSessionRepository.findByTokenHash(hashToken(rawToken))
                .ifPresent(session -> {
                    if (session.getRevokedAt() == null) {
                        session.setRevokedAt(Instant.now());
                        refreshTokenSessionRepository.save(session);
                    }
                });
    }

    @Transactional
    public void revokeUserSessions(String username) {
        refreshTokenSessionRepository.deleteByUserUsername(username);
    }

    @Transactional
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        refreshTokenSessionRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private RefreshTokenSession getValidSession(String rawToken) {
        RefreshTokenSession session = refreshTokenSessionRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid."));

        Instant now = Instant.now();
        if (session.getRevokedAt() != null || session.getExpiresAt().isBefore(now)) {
            throw new InvalidRefreshTokenException("Refresh token is no longer valid.");
        }

        return session;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record RefreshSession(String token, Instant expiresAt) {
    }

    public record RotatedRefreshSession(User user, String token, Instant expiresAt) {
    }
}
