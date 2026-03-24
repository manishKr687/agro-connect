package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

/**
 * Represents a purchase demand posted by a retailer.
 *
 * <p>Status lifecycle:
 * <pre>
 *   OPEN → RESERVED (when a DeliveryTask is created)
 *        → FULFILLED (task delivered successfully)
 *        → CANCELLED (task cancelled by admin)
 * </pre>
 *
 * <p>Only {@code OPEN} demands are visible to the matching engine and can be
 * edited or deleted directly. Once {@code RESERVED}, the retailer can only
 * submit a change request via the {@code requested*} fields, which must be
 * reviewed and approved by an admin.
 */
@Entity
@Table(name = "demands")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The retailer who posted this demand. */
    @ManyToOne
    @JoinColumn(name = "retailer_id")
    private User retailer;

    /** Type of crop requested (e.g. "Tomato"). Matched case-insensitively against harvest crop names. */
    private String cropName;

    /** Quantity needed in kilograms (or platform-defined unit). */
    private Double quantity;

    /** Latest acceptable date by which the crop must be delivered. */
    private LocalDate requiredDate;

    /** Maximum price the retailer is willing to pay per unit. */
    private Double targetPrice;

    // --- Change request fields (non-null when a pending change request exists) ---

    /** Requested new quantity. Null when no change request is pending. */
    private Double requestedQuantity;

    /** Requested new required date. Null when no change request is pending. */
    private LocalDate requestedRequiredDate;

    /** Requested new target price. Null when no change request is pending. */
    private Double requestedTargetPrice;

    /** Reason provided by the retailer when submitting a change request. */
    private String changeRequestReason;

    /** Current lifecycle status. Drives what operations are permitted on this demand. */
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * Lifecycle states for a demand.
     *
     * <ul>
     *   <li>{@code OPEN} — visible to matching engine; can be edited/deleted directly</li>
     *   <li>{@code RESERVED} — locked to an active delivery task; changes require admin approval</li>
     *   <li>{@code FULFILLED} — delivery completed; final terminal state</li>
     *   <li>{@code CANCELLED} — task cancelled by admin; demand re-enters circulation</li>
     * </ul>
     */
    public enum Status {
        OPEN, RESERVED, FULFILLED, CANCELLED
    }
}
