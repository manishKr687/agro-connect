package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "freshness_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FreshnessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private LocalDate harvestDate;
    private LocalDate deliveredDate;
    private Double freshnessHours;
}
