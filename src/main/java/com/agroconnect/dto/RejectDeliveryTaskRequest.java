package com.agroconnect.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectDeliveryTaskRequest {
    @NotBlank
    private String reason;
}
