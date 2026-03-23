package com.agroconnect.dto;

import com.agroconnect.model.Demand;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateDemandStatusRequest {
    @NotNull
    private Demand.Status status;
}
