package com.agroconnect.dto;

import com.agroconnect.model.DeliveryTask;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskExceptionResponse {
    private Long taskId;
    private String exceptionType;
    private String reason;
    private long ageHours;
    private DeliveryTask task;
}
