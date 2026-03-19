package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "approvals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Approval {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, UNDER_REVIEW, APPROVED, REJECTED, SUSPENDED

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private User reviewer; // Admin or agent

    private String reason; // rejection/suspension reason
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING, UNDER_REVIEW, APPROVED, REJECTED, SUSPENDED
    }
}
