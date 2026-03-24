package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Represents a crop harvest posted by a farmer.
 *
 * <p>Status lifecycle:
 * <pre>
 *   AVAILABLE → RESERVED (when a DeliveryTask is created)
 *             → WITHDRAWAL_REQUESTED (farmer wants to pull back a reserved harvest)
 *             → WITHDRAWN (admin cancels the task after withdrawal request)
 *             → SOLD (task delivered successfully)
 * </pre>
 *
 * <p>Only {@code AVAILABLE} harvests can be edited or deleted by the farmer.
 * The {@link com.agroconnect.service.MatchingService} only considers {@code AVAILABLE} harvests
 * when generating match suggestions.
 */
@Entity
@Table(name = "harvests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Harvest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The farmer who posted this harvest. */
    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private User farmer;

    /** Type of crop (e.g. "Tomato"). Matched case-insensitively against demand crop names. */
    private String cropName;

    /** Available quantity in kilograms (or platform-defined unit). */
    private Double quantity;

    /** Date the crop was or will be harvested. Used in freshness score calculation. */
    private LocalDate harvestDate;

    /** Farmer's asking price per unit. Compared against the demand's target price in scoring. */
    private Double expectedPrice;

    /** Current lifecycle status. Drives what operations are permitted on this harvest. */
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * Lifecycle states for a harvest.
     *
     * <ul>
     *   <li>{@code AVAILABLE} — visible to the matching engine; can be edited/deleted</li>
     *   <li>{@code RESERVED} — locked to an active delivery task</li>
     *   <li>{@code WITHDRAWAL_REQUESTED} — farmer wants to cancel while RESERVED</li>
     *   <li>{@code WITHDRAWN} — withdrawal approved; harvest is removed from circulation</li>
     *   <li>{@code SOLD} — delivery completed; final terminal state</li>
     * </ul>
     */
    public enum Status {
        AVAILABLE, RESERVED, SOLD, WITHDRAWAL_REQUESTED, WITHDRAWN
    }
}
