package com.agroconnect.controller;

import com.agroconnect.dto.RejectDeliveryTaskRequest;
import com.agroconnect.model.DeliveryTask;
import com.agroconnect.service.DeliveryTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent-scoped delivery task endpoints under {@code /api/agents/{agentId}/tasks}.
 *
 * <p>The {@code agentId} path variable must match the authenticated user's ID and role (AGENT),
 * enforced by {@link com.agroconnect.service.AccessControlService#requireCurrentUser}.
 * Additionally, each task action verifies that the task is actually assigned to this agent.
 *
 * <p>Endpoints drive the agent-side status progression:
 * {@code ASSIGNED → ACCEPTED → PICKED_UP → IN_TRANSIT → DELIVERED}
 * or {@code ASSIGNED → REJECTED}.
 */
@RestController
@RequestMapping("/api/agents/{agentId}/tasks")
@RequiredArgsConstructor
public class AgentDeliveryTaskController {
    private final DeliveryTaskService deliveryTaskService;

    @GetMapping
    public List<DeliveryTask> getAssignedTasks(@PathVariable Long agentId) {
        return deliveryTaskService.getTasksAssignedToAgent(agentId);
    }

    @PostMapping("/{taskId}/accept")
    public DeliveryTask acceptTask(@PathVariable Long agentId, @PathVariable Long taskId) {
        return deliveryTaskService.acceptTask(agentId, taskId);
    }

    @PostMapping("/{taskId}/reject")
    public DeliveryTask rejectTask(
            @PathVariable Long agentId,
            @PathVariable Long taskId,
            @Valid @RequestBody RejectDeliveryTaskRequest request
    ) {
        return deliveryTaskService.rejectTask(agentId, taskId, request.getReason());
    }

    @PostMapping("/{taskId}/pickup")
    public DeliveryTask markPickedUp(@PathVariable Long agentId, @PathVariable Long taskId) {
        return deliveryTaskService.markPickedUp(agentId, taskId);
    }

    @PostMapping("/{taskId}/in-transit")
    public DeliveryTask markInTransit(@PathVariable Long agentId, @PathVariable Long taskId) {
        return deliveryTaskService.markInTransit(agentId, taskId);
    }

    @PostMapping("/{taskId}/deliver")
    public DeliveryTask markDelivered(@PathVariable Long agentId, @PathVariable Long taskId) {
        return deliveryTaskService.markDelivered(agentId, taskId);
    }
}
