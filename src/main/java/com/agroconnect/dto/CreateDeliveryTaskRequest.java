package com.agroconnect.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateDeliveryTaskRequest {
    @NotNull
    private Long adminId;

    private Long agentId;

    @NotNull
    private Long harvestId;

    @NotNull
    private Long demandId;
}
