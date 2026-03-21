package com.agroconnect.service;

import com.agroconnect.dto.CreateDeliveryTaskRequest;
import com.agroconnect.dto.UpdateDeliveryTaskRequest;
import com.agroconnect.model.DeliveryTask;
import com.agroconnect.model.Demand;
import com.agroconnect.model.Harvest;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.DeliveryTaskRepository;
import com.agroconnect.repository.DemandRepository;
import com.agroconnect.repository.HarvestRepository;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeliveryTaskService {
    private final DeliveryTaskRepository deliveryTaskRepository;
    private final HarvestRepository harvestRepository;
    private final DemandRepository demandRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;

    public DeliveryTask createTask(CreateDeliveryTaskRequest request) {
        User admin = accessControlService.requireAdmin(request.getAdminId());
        User agent = getUserWithRole(request.getAgentId(), Role.AGENT, "User is not an agent");
        Harvest harvest = harvestRepository.findById(request.getHarvestId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Harvest not found"));
        Demand demand = demandRepository.findById(request.getDemandId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found"));

        if (harvest.getStatus() != Harvest.Status.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Harvest is not available for assignment");
        }

        if (demand.getStatus() != Demand.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demand is not open for assignment");
        }

        harvest.setStatus(Harvest.Status.RESERVED);
        demand.setStatus(Demand.Status.RESERVED);
        harvestRepository.save(harvest);
        demandRepository.save(demand);

        DeliveryTask task = DeliveryTask.builder()
                .harvest(harvest)
                .demand(demand)
                .assignedAgent(agent)
                .assignedBy(admin)
                .status(DeliveryTask.Status.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .build();

        return deliveryTaskRepository.save(task);
    }

    public List<DeliveryTask> getTasksAssignedToAgent(Long agentId) {
        accessControlService.requireCurrentUser(agentId, Role.AGENT);
        return deliveryTaskRepository.findByAssignedAgentId(agentId);
    }

    public List<DeliveryTask> getAllTasksForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);
        return deliveryTaskRepository.findAll();
    }

    public DeliveryTask acceptTask(Long agentId, Long taskId) {
        DeliveryTask task = getAssignedTask(agentId, taskId);
        ensureStatus(task, DeliveryTask.Status.ASSIGNED, "Only assigned tasks can be accepted");
        task.setStatus(DeliveryTask.Status.ACCEPTED);
        task.setAcceptedAt(LocalDateTime.now());
        task.setRejectionReason(null);
        return deliveryTaskRepository.save(task);
    }

    public DeliveryTask rejectTask(Long agentId, Long taskId, String reason) {
        DeliveryTask task = getAssignedTask(agentId, taskId);
        ensureStatus(task, DeliveryTask.Status.ASSIGNED, "Only assigned tasks can be rejected");
        task.setStatus(DeliveryTask.Status.REJECTED);
        task.setRejectionReason(reason);

        Harvest harvest = task.getHarvest();
        Demand demand = task.getDemand();
        harvest.setStatus(Harvest.Status.AVAILABLE);
        demand.setStatus(Demand.Status.OPEN);
        harvestRepository.save(harvest);
        demandRepository.save(demand);

        return deliveryTaskRepository.save(task);
    }

    public DeliveryTask markPickedUp(Long agentId, Long taskId) {
        DeliveryTask task = getAssignedTask(agentId, taskId);
        ensureStatus(task, DeliveryTask.Status.ACCEPTED, "Only accepted tasks can be picked up");
        task.setStatus(DeliveryTask.Status.PICKED_UP);
        task.setPickedUpAt(LocalDateTime.now());
        return deliveryTaskRepository.save(task);
    }

    public DeliveryTask markInTransit(Long agentId, Long taskId) {
        DeliveryTask task = getAssignedTask(agentId, taskId);
        ensureStatus(task, DeliveryTask.Status.PICKED_UP, "Only picked up tasks can move to in transit");
        task.setStatus(DeliveryTask.Status.IN_TRANSIT);
        task.setInTransitAt(LocalDateTime.now());
        return deliveryTaskRepository.save(task);
    }

    public DeliveryTask markDelivered(Long agentId, Long taskId) {
        DeliveryTask task = getAssignedTask(agentId, taskId);
        ensureStatus(task, DeliveryTask.Status.IN_TRANSIT, "Only in-transit tasks can be delivered");
        task.setStatus(DeliveryTask.Status.DELIVERED);
        task.setDeliveredAt(LocalDateTime.now());

        Harvest harvest = task.getHarvest();
        Demand demand = task.getDemand();
        harvest.setStatus(Harvest.Status.SOLD);
        demand.setStatus(Demand.Status.FULFILLED);
        harvestRepository.save(harvest);
        demandRepository.save(demand);

        return deliveryTaskRepository.save(task);
    }

    public DeliveryTask updateTaskForAdmin(Long adminId, Long taskId, UpdateDeliveryTaskRequest request) {
        accessControlService.requireAdmin(adminId);
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
        User agent = getUserWithRole(request.getAgentId(), Role.AGENT, "User is not an agent");

        task.setAssignedAgent(agent);
        return deliveryTaskRepository.save(task);
    }

    public void deleteTaskForAdmin(Long adminId, Long taskId) {
        accessControlService.requireAdmin(adminId);
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (task.getStatus() != DeliveryTask.Status.DELIVERED) {
            Harvest harvest = task.getHarvest();
            Demand demand = task.getDemand();
            harvest.setStatus(Harvest.Status.AVAILABLE);
            demand.setStatus(Demand.Status.OPEN);
            harvestRepository.save(harvest);
            demandRepository.save(demand);
        }

        deliveryTaskRepository.delete(task);
    }

    private DeliveryTask getAssignedTask(Long agentId, Long taskId) {
        accessControlService.requireCurrentUser(agentId, Role.AGENT);
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (!task.getAssignedAgent().getId().equals(agentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Task is not assigned to this agent");
        }

        return task;
    }

    private void ensureStatus(DeliveryTask task, DeliveryTask.Status expected, String message) {
        if (task.getStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private User getUserWithRole(Long userId, Role role, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return user;
    }
}
