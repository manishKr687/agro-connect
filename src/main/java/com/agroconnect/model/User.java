
package com.agroconnect.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import com.agroconnect.model.enums.Role;

/**
 * Represents a platform user.
 *
 * <p>All four roles (ADMIN, FARMER, RETAILER, AGENT) share this entity.
 * The {@code role} field drives access control throughout the application —
 * every service method delegates to {@link com.agroconnect.service.AccessControlService}
 * to verify that the caller holds the required role before proceeding.
 *
 * <p>The {@code password} field is BCrypt-hashed and write-only in JSON responses
 * to prevent it from leaking through the API.
 */
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

    /** Display name shown in the UI. */
    @Column(nullable = false)
    private String name;

    /** BCrypt-hashed password. Never serialised into API responses. */
    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** Recovery email used for password reset links. */
    @Column(unique = true)
    private String email;

    /** Phone number used as the login handle and for password reset OTPs. */
    @Column(name = "phone_number", unique = true, nullable = false)
    private String phoneNumber;

    /** Determines what actions this user can perform. Stored as a string in the DB. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
