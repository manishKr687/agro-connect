package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

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

    @ManyToOne
    @JoinColumn(name = "farmer_id")
    private User farmer;

    private String cropName;
    private Double quantity;
    private LocalDate harvestDate;
    private Double expectedPrice;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        AVAILABLE, RESERVED, SOLD, WITHDRAWAL_REQUESTED, WITHDRAWN
    }
}
