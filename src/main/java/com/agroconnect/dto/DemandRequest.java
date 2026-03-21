package com.agroconnect.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DemandRequest {
    @NotBlank
    private String cropName;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private Double quantity;

    @NotNull
    @FutureOrPresent
    private LocalDate requiredDate;

    @NotNull
    @DecimalMin(value = "0.0")
    private Double targetPrice;
}
