package com.agroconnect.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "harvest_id")
    private Harvest harvest;

    @ManyToOne(optional = false)
    @JoinColumn(name = "demand_id")
    private Demand demand;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by_id")
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime pickedUpAt;
    private LocalDateTime inTransitAt;
    private LocalDateTime deliveredAt;
    private String rejectionReason;

    public enum Status {
        ASSIGNED,
        ACCEPTED,
        REJECTED,
        PICKED_UP,
        IN_TRANSIT,
        DELIVERED
    }
}
