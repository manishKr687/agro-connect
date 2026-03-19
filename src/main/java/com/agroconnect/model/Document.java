package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private Type type; // ID_PROOF, BUSINESS_DOC, OTHER

    private String url; // File storage location

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, VERIFIED, REJECTED

    private LocalDateTime uploadedAt;

    public enum Type {
        ID_PROOF, BUSINESS_DOC, OTHER
    }

    public enum Status {
        PENDING, VERIFIED, REJECTED
    }
}
