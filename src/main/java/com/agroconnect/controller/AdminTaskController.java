package com.agroconnect.controller;

import com.agroconnect.model.DeliveryTask;
import com.agroconnect.service.DeliveryTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
public class AdminTaskController {
    private final DeliveryTaskService deliveryTaskService;

    @GetMapping("/{taskId}")
    public DeliveryTask getTaskDetails(@PathVariable Long taskId) {
        return deliveryTaskService.getTaskDetailsForAdmin(taskId);
    }
}
