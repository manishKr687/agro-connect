package com.agroconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelTaskRequest {
    @NotBlank
    @Size(max = 500)
    private String reason;
}
