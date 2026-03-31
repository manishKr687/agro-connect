package com.agroconnect.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token revocation store.
 *
 * <p>Two revocation mechanisms:
 * <ul>
 *   <li><b>Token blacklist</b> — individual tokens added on logout. Keyed by token string,
 *       value is the token's expiry so stale entries can be purged.</li>
 *   <li><b>Username revocation</b> — set on user deletion. Any token for that username
 *       issued <em>before</em> the revocation timestamp is rejected, invalidating all
 *       outstanding tokens without needing to enumerate them.</li>
 * </ul>
 *
 * <p>Expired entries are purged every hour by {@link #purgeExpired()} to prevent unbounded growth.
 */
@Service
public class TokenBlacklistService {

    /** token string → expiry instant */
    private final ConcurrentHashMap<String, Instant> blacklistedTokens = new ConcurrentHashMap<>();

    /** username → revocation instant (all tokens issued before this time are invalid) */
    private final ConcurrentHashMap<String, Instant> revokedUsers = new ConcurrentHashMap<>();

    /** Blacklists a specific token (used on logout). */
    public void blacklistToken(String token, Instant expiry) {
        blacklistedTokens.put(token, expiry);
    }

    /** Returns true if this exact token has been blacklisted. */
    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }

    /** Marks all tokens for a username as revoked from this moment (used on user deletion). */
    public void revokeUser(String username) {
        revokedUsers.put(username, Instant.now());
    }

    /**
     * Returns true if the token was issued before the user's revocation timestamp,
     * meaning it was invalidated by a user deletion.
     */
    public boolean isUserRevoked(String username, Instant tokenIssuedAt) {
        Instant revokedAt = revokedUsers.get(username);
        return revokedAt != null && tokenIssuedAt.isBefore(revokedAt);
    }

    /** Removes expired token entries every hour to prevent memory leaks. */
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        Instant now = Instant.now();
        blacklistedTokens.entrySet().removeIf(e -> e.getValue().isBefore(now));
    }
}
