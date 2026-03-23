package com.agroconnect.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApproveAssignmentRequest {
    @NotNull
    private Long harvestId;

    @NotNull
    private Long demandId;
}
