package com.agroconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelTaskRequest {
    @NotBlank
    private String reason;
}
