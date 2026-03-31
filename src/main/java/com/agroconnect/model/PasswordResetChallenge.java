package com.agroconnect.model;

import com.agroconnect.model.enums.PasswordResetChannel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "password_reset_challenges")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PasswordResetChannel channel;

    @Column(nullable = false)
    private String identifier;

    @Column(name = "secret_hash", nullable = false, length = 128)
    private String secretHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
