package com.agroconnect.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

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

    @ManyToOne
    @JoinColumn(name = "retailer_id")
    private User retailer;

    private String cropName;
    private Double quantity;
    private LocalDate requiredDate;
    private Double targetPrice;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        OPEN, RESERVED, FULFILLED, CANCELLED
    }
}
