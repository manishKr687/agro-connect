package com.agroconnect.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "revoked_users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevokedUser {

    @Id
    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;
}
