package com.agroconnect.dto;

import com.agroconnect.model.Harvest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateHarvestStatusRequest {
    @NotNull
    private Harvest.Status status;
}
