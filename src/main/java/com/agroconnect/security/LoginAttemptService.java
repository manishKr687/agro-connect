package com.agroconnect.security;

import com.agroconnect.model.LoginAttemptRecord;
import com.agroconnect.repository.LoginAttemptRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Tracks failed login attempts per username and persists lockout state in the database.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    @Value("${app.security.max-login-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private long lockoutMinutes;

    private final LoginAttemptRecordRepository loginAttemptRecordRepository;

    public void recordFailure(String username) {
        Instant now = Instant.now();
        LoginAttemptRecord record = loginAttemptRecordRepository.findById(username)
                .orElseGet(() -> LoginAttemptRecord.builder()
                        .username(username)
                        .failureCount(0)
                        .updatedAt(now)
                        .build());

        int failures = record.getFailureCount() + 1;
        record.setFailureCount(failures);
        record.setUpdatedAt(now);

        if (failures >= maxAttempts) {
            record.setLockedUntil(now.plusSeconds(lockoutMinutes * 60));
        }

        loginAttemptRecordRepository.save(record);
    }

    public void recordSuccess(String username) {
        loginAttemptRecordRepository.deleteById(username);
    }

    public boolean isLocked(String username) {
        LoginAttemptRecord record = loginAttemptRecordRepository.findById(username).orElse(null);
        if (record == null || record.getLockedUntil() == null) {
            return false;
        }

        if (Instant.now().isAfter(record.getLockedUntil())) {
            loginAttemptRecordRepository.deleteById(username);
            return false;
        }

        return true;
    }

    public long secondsUntilUnlock(String username) {
        LoginAttemptRecord record = loginAttemptRecordRepository.findById(username).orElse(null);
        if (record == null || record.getLockedUntil() == null) {
            return 0;
        }

        long remaining = record.getLockedUntil().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(remaining, 0);
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        loginAttemptRecordRepository.deleteUnlockedOrExpired(Instant.now());
    }
}
