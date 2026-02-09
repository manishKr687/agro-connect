package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "harvest_id")
    private Harvest harvest;

    @ManyToOne
    @JoinColumn(name = "demand_id")
    private Demand demand;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private User admin;

    @ManyToOne
    @JoinColumn(name = "mediator_id")
    private User mediator;

    private Double normalizedPrice;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime createdAt;
    private LocalDateTime collectedAt;
    private LocalDateTime deliveredAt;

    public enum Status {
        CREATED, COLLECTED, DELIVERED, CLOSED, FLAGGED
    }
}
