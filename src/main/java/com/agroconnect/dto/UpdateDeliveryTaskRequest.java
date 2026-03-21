package com.agroconnect.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDeliveryTaskRequest {
    @NotNull
    private Long agentId;
}
