package com.agroconnect.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks failed login attempts per username and enforces a temporary lockout.
 *
 * <p>After {@code app.security.max-login-attempts} consecutive failures the account
 * is locked for {@code app.security.lockout-duration-minutes} minutes.
 * On a successful login the failure counter is reset.
 *
 * <p>Stale entries (unlocked accounts that haven't been touched) are purged every hour.
 *
 * <p>Defaults: 5 attempts, 15-minute lockout.
 */
@Service
public class LoginAttemptService {

    @Value("${app.security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private long lockoutMinutes;

    private static class AttemptRecord {
        final AtomicInteger count = new AtomicInteger(0);
        volatile Instant lockedUntil = null;
    }

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    /** Called on every failed login for the given username. */
    public void recordFailure(String username) {
        AttemptRecord record = attempts.computeIfAbsent(username, k -> new AttemptRecord());
        int failures = record.count.incrementAndGet();
        if (failures >= maxAttempts) {
            record.lockedUntil = Instant.now().plusSeconds(lockoutMinutes * 60);
        }
    }

    /** Called on successful login — resets the counter. */
    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    /**
     * Returns true if the username is currently locked out.
     * Automatically unlocks if the lockout window has passed.
     */
    public boolean isLocked(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null || record.lockedUntil == null) return false;

        if (Instant.now().isAfter(record.lockedUntil)) {
            attempts.remove(username); // lockout expired — clear it
            return false;
        }
        return true;
    }

    /** Returns how many seconds remain in the lockout, or 0 if not locked. */
    public long secondsUntilUnlock(String username) {
        AttemptRecord record = attempts.get(username);
        if (record == null || record.lockedUntil == null) return 0;
        long remaining = record.lockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(remaining, 0);
    }

    /** Removes stale unlocked entries every hour. */
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(e ->
            e.getValue().lockedUntil == null || now.isAfter(e.getValue().lockedUntil)
        );
    }
}
