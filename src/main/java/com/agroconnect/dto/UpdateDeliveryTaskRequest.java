package com.agroconnect.dto;

import com.agroconnect.model.DeliveryTask;
import lombok.Data;

@Data
public class UpdateDeliveryTaskRequest {
    private Long agentId;

    private DeliveryTask.Status status;
}
