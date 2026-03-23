package com.agroconnect.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DemandChangeRequest {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private Double quantity;

    @NotNull
    @FutureOrPresent
    private LocalDate requiredDate;

    @NotNull
    @DecimalMin(value = "0.0")
    private Double targetPrice;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
