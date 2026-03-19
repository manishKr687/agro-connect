package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Verification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Type type; // PHONE, DOCUMENT, BUSINESS

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, VERIFIED, REJECTED

    private String data; // e.g., phone, doc url, business id
    private String reason; // rejection reason if any
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum Type {
        PHONE, DOCUMENT, BUSINESS
    }

    public enum Status {
        PENDING, VERIFIED, REJECTED
    }
}
