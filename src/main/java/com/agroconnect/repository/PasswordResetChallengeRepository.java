package com.agroconnect.repository;

import com.agroconnect.model.PasswordResetChallenge;
import com.agroconnect.model.enums.PasswordResetChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallenge, Long> {
    void deleteByUserIdAndChannelAndUsedAtIsNull(Long userId, PasswordResetChannel channel);

    List<PasswordResetChallenge> findByChannelAndIdentifierAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            PasswordResetChannel channel,
            String identifier,
            Instant now
    );

    void deleteByExpiresAtBefore(Instant now);
}
