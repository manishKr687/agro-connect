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

    private String vegetableType;
    private Double quantity;
    private LocalDate requiredDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        PENDING, MATCHED, COMPLETED
    }
}
