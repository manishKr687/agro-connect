
package com.agroconnect.model;


import jakarta.persistence.*;
import lombok.*;
import com.agroconnect.model.enums.Role;
import com.agroconnect.model.enums.Status;

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

    @Enumerated(EnumType.STRING)
    private Status status; // PENDING, UNDER_REVIEW, APPROVED, REJECTED, SUSPENDED

    @Column(unique = true)
    private String phone;
    private String passwordHash;
    private String languagePreference;

    // For assisted onboarding (Farmers)
    private Long agentId;

    // For Retailers
    private String businessName;

    // Role and Status enums moved to com.agroconnect.model.enum
}
