package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(unique = true)
    private String phone;
    private String passwordHash;
    private String languagePreference;

    public enum Role {
        ADMIN, FARMER, MEDIATOR, RETAILER
    }
}
