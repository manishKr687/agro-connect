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

/**
 * Represents a delivery task that links a matched harvest to a demand and assigns
 * an agent to physically collect and deliver the crop.
 *
 * <p>Status lifecycle (agent-driven path):
 * <pre>
 *   ASSIGNED → ACCEPTED → PICKED_UP → IN_TRANSIT → DELIVERED
 *           ↘ REJECTED (agent rejects; harvest/demand revert to AVAILABLE/OPEN)
 *
 *   Any active status → CANCELLED (admin; harvest/demand revert)
 * </pre>
 *
 * <p>Each status transition records a timestamp ({@code assignedAt}, {@code acceptedAt}, etc.)
 * so the admin can monitor delivery speed and flag tasks stuck for over 24 hours.
 *
 * <p>A task is considered <em>active</em> while its status is one of:
 * ASSIGNED, ACCEPTED, PICKED_UP, IN_TRANSIT.
 */
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

    /** The harvest being delivered. Set to RESERVED on task creation; SOLD on delivery. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "harvest_id")
    private Harvest harvest;

    /** The demand being fulfilled. Set to RESERVED on task creation; FULFILLED on delivery. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "demand_id")
    private Demand demand;

    /** The agent responsible for collecting and delivering the crop. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_agent_id")
    private User assignedAgent;

    /** The admin who created this task. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "assigned_by_id")
    private User assignedBy;

    /** Current lifecycle status. */
    @Enumerated(EnumType.STRING)
    private Status status;

    /** When the task was created and assigned to the agent. */
    private LocalDateTime assignedAt;

    /** When the agent accepted the task. */
    private LocalDateTime acceptedAt;

    /** When the agent picked up the crop from the farmer. */
    private LocalDateTime pickedUpAt;

    /** When the agent started transit to the retailer. */
    private LocalDateTime inTransitAt;

    /** When the agent confirmed delivery to the retailer. */
    private LocalDateTime deliveredAt;

    /**
     * Reason recorded when the task is rejected (by agent), cancelled (by admin),
     * or when a farmer requests harvest withdrawal. Reused across exception types
     * to keep the schema simple.
     */
    private String rejectionReason;

    /**
     * Lifecycle states for a delivery task.
     *
     * <ul>
     *   <li>{@code ASSIGNED} — task created; waiting for agent to accept</li>
     *   <li>{@code ACCEPTED} — agent confirmed; waiting for pickup</li>
     *   <li>{@code PICKED_UP} — agent has the crop</li>
     *   <li>{@code IN_TRANSIT} — agent is en route to retailer</li>
     *   <li>{@code DELIVERED} — delivery confirmed; harvest=SOLD, demand=FULFILLED</li>
     *   <li>{@code REJECTED} — agent declined; harvest/demand reverted to AVAILABLE/OPEN</li>
     *   <li>{@code CANCELLED} — admin cancelled; harvest/demand reverted</li>
     * </ul>
     */
    public enum Status {
        ASSIGNED,
        ACCEPTED,
        REJECTED,
        PICKED_UP,
        IN_TRANSIT,
        DELIVERED,
        CANCELLED
    }
}
