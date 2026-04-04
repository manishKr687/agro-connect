package com.agroconnect.service;

import com.agroconnect.dto.ApproveAssignmentRequest;
import com.agroconnect.dto.CancelTaskRequest;
import com.agroconnect.dto.CreateDeliveryTaskRequest;
import com.agroconnect.dto.ReassignTaskRequest;
import com.agroconnect.dto.TaskExceptionResponse;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Manages the full lifecycle of delivery tasks — from creation through delivery or cancellation.
 *
 * <h2>Task creation</h2>
 * <p>An admin selects a matched harvest–demand pair and calls {@link #createTask}.
 * The service reserves both the harvest and demand, then assigns an agent (either
 * explicitly or by selecting the least-loaded available agent automatically).
 *
 * <h2>Agent-driven status progression</h2>
 * <pre>
 *   ASSIGNED → ACCEPTED (agent accepts)
 *            → PICKED_UP (agent picks up crop)
 *            → IN_TRANSIT (agent departs for retailer)
 *            → DELIVERED (agent confirms delivery; harvest=SOLD, demand=FULFILLED)
 *            → REJECTED (agent declines; harvest/demand revert to AVAILABLE/OPEN)
 * </pre>
 *
 * <h2>Admin overrides</h2>
 * <p>Admins can cancel any non-terminal task, reassign the agent, force-update the status,
 * retry a rejected/cancelled task, or permanently delete a task record.
 *
 * <h2>Exception monitoring</h2>
 * <p>{@link #getTaskExceptionsForAdmin} surfaces four categories of exceptions:
 * AGENT_REJECTED, FARMER_WITHDRAWAL_REQUEST, RETAILER_DEMAND_CHANGE_REQUEST, DELIVERY_STUCK (&gt;24h active).
 */
@Service
@RequiredArgsConstructor
public class DeliveryTaskService {
    private static final List<DeliveryTask.Status> ACTIVE_TASK_STATUSES = DeliveryTask.ACTIVE_STATUSES;

    private final DeliveryTaskRepository deliveryTaskRepository;
    private final HarvestRepository harvestRepository;
    private final DemandRepository demandRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;

    /**
     * Creates a delivery task for the given harvest–demand pair.
     *
     * <p>If no {@code agentId} is specified in the request, the least-loaded available agent is
     * selected automatically (fewest active tasks, then fewest total tasks, then lowest ID).
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 if harvest is not AVAILABLE or demand is not OPEN
     * @throws org.springframework.web.server.ResponseStatusException 409 if either harvest or demand already has an active task
     * @throws org.springframework.web.server.ResponseStatusException 400 if no agents are available
     */
    @Transactional
    public DeliveryTask createTask(CreateDeliveryTaskRequest request) {
        User admin = accessControlService.requireAdmin(request.getAdminId());
        User agent = request.getAgentId() != null
                ? getUserWithRole(request.getAgentId(), Role.AGENT, "User is not an agent")
                : findLeastLoadedAgent();
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

        ensureNoActiveAssignmentConflicts(harvest.getId(), demand.getId());

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

    @Transactional
    public DeliveryTask approveAssignment(Long adminId, ApproveAssignmentRequest request) {
        CreateDeliveryTaskRequest createRequest = new CreateDeliveryTaskRequest();
        createRequest.setAdminId(adminId);
        createRequest.setHarvestId(request.getHarvestId());
        createRequest.setDemandId(request.getDemandId());
        return createTask(createRequest);
    }

    public List<DeliveryTask> getTasksAssignedToAgent(Long agentId) {
        accessControlService.requireCurrentUser(agentId, Role.AGENT);
        return deliveryTaskRepository.findByAssignedAgentId(agentId);
    }

    public List<DeliveryTask> getAllTasksForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);
        return deliveryTaskRepository.findAll();
    }

    public DeliveryTask getTaskDetailsForAdmin(Long taskId) {
        accessControlService.requireAdmin();
        return deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));
    }

    /**
     * Returns all tasks that require admin attention, sorted by age (oldest first).
     *
     * <p>Four exception types are surfaced:
     * <ul>
     *   <li>{@code AGENT_REJECTED} — agent declined the task</li>
     *   <li>{@code FARMER_WITHDRAWAL_REQUEST} — farmer wants to pull back a reserved harvest</li>
     *   <li>{@code RETAILER_DEMAND_CHANGE_REQUEST} — retailer submitted changes to a reserved demand</li>
     *   <li>{@code DELIVERY_STUCK} — task has been active for more than 24 hours</li>
     * </ul>
     */
    public List<TaskExceptionResponse> getTaskExceptionsForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime stuckThreshold = now.minusHours(24);

        return deliveryTaskRepository.findExceptionCandidates(
                DeliveryTask.Status.REJECTED,
                Harvest.Status.WITHDRAWAL_REQUESTED,
                ACTIVE_TASK_STATUSES,
                stuckThreshold).stream()
                .map(task -> toTaskException(task, now))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(TaskExceptionResponse::getAgeHours).reversed())
                .toList();
    }

    @Transactional
    public DeliveryTask acceptTask(Long agentId, Long taskId) {
        DeliveryTask task = getAssignedTask(agentId, taskId);
        ensureStatus(task, DeliveryTask.Status.ASSIGNED, "Only assigned tasks can be accepted");
        task.setStatus(DeliveryTask.Status.ACCEPTED);
        task.setAcceptedAt(LocalDateTime.now());
        task.setRejectionReason(null);
        return deliveryTaskRepository.save(task);
    }

    @Transactional
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

    @Transactional
    public DeliveryTask markPickedUp(Long agentId, Long taskId) {
        DeliveryTask task = getAssignedTask(agentId, taskId);
        ensureStatus(task, DeliveryTask.Status.ACCEPTED, "Only accepted tasks can be picked up");
        task.setStatus(DeliveryTask.Status.PICKED_UP);
        task.setPickedUpAt(LocalDateTime.now());
        return deliveryTaskRepository.save(task);
    }

    @Transactional
    public DeliveryTask markInTransit(Long agentId, Long taskId) {
        DeliveryTask task = getAssignedTask(agentId, taskId);
        ensureStatus(task, DeliveryTask.Status.PICKED_UP, "Only picked up tasks can move to in transit");
        task.setStatus(DeliveryTask.Status.IN_TRANSIT);
        task.setInTransitAt(LocalDateTime.now());
        return deliveryTaskRepository.save(task);
    }

    @Transactional
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

    @Transactional
    public DeliveryTask reassignTaskForAdmin(Long adminId, Long taskId, ReassignTaskRequest request) {
        accessControlService.requireAdmin(adminId);
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (!ACTIVE_TASK_STATUSES.contains(task.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only active tasks can be reassigned");
        }
        if (task.getHarvest().getStatus() == Harvest.Status.WITHDRAWAL_REQUESTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task cannot be reassigned while farmer withdrawal is pending");
        }

        User agent = request.getAgentId() != null
                ? getUserWithRole(request.getAgentId(), Role.AGENT, "User is not an agent")
                : findLeastLoadedAgent(task.getAssignedAgent() == null ? null : task.getAssignedAgent().getId());

        task.setAssignedAgent(agent);
        task.setAssignedAt(LocalDateTime.now());
        task.setRejectionReason(null);
        return deliveryTaskRepository.save(task);
    }

    @Transactional
    public DeliveryTask updateTaskForAdmin(Long adminId, Long taskId, UpdateDeliveryTaskRequest request) {
        accessControlService.requireAdmin(adminId);
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (request.getAgentId() == null && request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide agentId or status to update the task");
        }

        if (request.getAgentId() != null) {
            User agent = getUserWithRole(request.getAgentId(), Role.AGENT, "User is not an agent");
            task.setAssignedAgent(agent);
        }

        if (request.getStatus() != null) {
            applyAdminStatusUpdate(task, request.getStatus());
        }

        return deliveryTaskRepository.save(task);
    }

    @Transactional
    public DeliveryTask cancelTaskForAdmin(Long adminId, Long taskId, CancelTaskRequest request) {
        accessControlService.requireAdmin(adminId);
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (task.getStatus() == DeliveryTask.Status.DELIVERED || task.getStatus() == DeliveryTask.Status.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task cannot be cancelled in its current state");
        }

        task.setStatus(DeliveryTask.Status.CANCELLED);
        task.setRejectionReason(request.getReason());
        resetTaskTimeline(task);

        Harvest harvest = task.getHarvest();
        Demand demand = task.getDemand();
        harvest.setStatus(harvest.getStatus() == Harvest.Status.WITHDRAWAL_REQUESTED ? Harvest.Status.WITHDRAWN : Harvest.Status.AVAILABLE);
        demand.setStatus(Demand.Status.OPEN);
        harvestRepository.save(harvest);
        demandRepository.save(demand);

        return deliveryTaskRepository.save(task);
    }

    /**
     * Re-creates a delivery task for the same harvest–demand pair from a rejected or cancelled task.
     *
     * <p>If the original task was {@code REJECTED}, it is first transitioned to {@code CANCELLED}
     * before the new task is created, ensuring only one active task exists per harvest/demand at a time.
     * The new task auto-assigns the least-loaded agent.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 if the task is not in REJECTED or CANCELLED state
     */
    @Transactional
    public DeliveryTask retryTaskForAdmin(Long adminId, Long taskId) {
        accessControlService.requireAdmin(adminId);
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Task not found"));

        if (task.getStatus() != DeliveryTask.Status.REJECTED && task.getStatus() != DeliveryTask.Status.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only rejected or cancelled tasks can be retried");
        }

        if (task.getStatus() == DeliveryTask.Status.REJECTED) {
            task.setStatus(DeliveryTask.Status.CANCELLED);
            task.setRejectionReason("Retried by admin");
            deliveryTaskRepository.save(task);
        }

        CreateDeliveryTaskRequest createRequest = new CreateDeliveryTaskRequest();
        createRequest.setAdminId(adminId);
        createRequest.setHarvestId(task.getHarvest().getId());
        createRequest.setDemandId(task.getDemand().getId());
        return createTask(createRequest);
    }

    @Transactional
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

    private void applyAdminStatusUpdate(DeliveryTask task, DeliveryTask.Status status) {
        LocalDateTime now = LocalDateTime.now();
        task.setStatus(status);

        switch (status) {
            case ASSIGNED -> {
                resetTaskTimeline(task);
                task.setAssignedAt(task.getAssignedAt() == null ? now : task.getAssignedAt());
                task.setRejectionReason(null);
                setTaskReservationState(task);
            }
            case ACCEPTED -> {
                resetTaskTimeline(task);
                task.setAssignedAt(task.getAssignedAt() == null ? now : task.getAssignedAt());
                task.setAcceptedAt(now);
                task.setRejectionReason(null);
                setTaskReservationState(task);
            }
            case PICKED_UP -> {
                resetTaskTimeline(task);
                task.setAssignedAt(task.getAssignedAt() == null ? now : task.getAssignedAt());
                task.setAcceptedAt(now);
                task.setPickedUpAt(now);
                task.setRejectionReason(null);
                setTaskReservationState(task);
            }
            case IN_TRANSIT -> {
                resetTaskTimeline(task);
                task.setAssignedAt(task.getAssignedAt() == null ? now : task.getAssignedAt());
                task.setAcceptedAt(now);
                task.setPickedUpAt(now);
                task.setInTransitAt(now);
                task.setRejectionReason(null);
                setTaskReservationState(task);
            }
            case DELIVERED -> {
                resetTaskTimeline(task);
                task.setAssignedAt(task.getAssignedAt() == null ? now : task.getAssignedAt());
                task.setAcceptedAt(now);
                task.setPickedUpAt(now);
                task.setInTransitAt(now);
                task.setDeliveredAt(now);
                task.setRejectionReason(null);
                task.getHarvest().setStatus(Harvest.Status.SOLD);
                task.getDemand().setStatus(Demand.Status.FULFILLED);
                harvestRepository.save(task.getHarvest());
                demandRepository.save(task.getDemand());
            }
            case REJECTED -> {
                resetTaskTimeline(task);
                task.setAssignedAt(task.getAssignedAt() == null ? now : task.getAssignedAt());
                task.setRejectionReason("Rejected by admin");
                task.getHarvest().setStatus(Harvest.Status.AVAILABLE);
                task.getDemand().setStatus(Demand.Status.OPEN);
                harvestRepository.save(task.getHarvest());
                demandRepository.save(task.getDemand());
            }
            case CANCELLED -> {
                resetTaskTimeline(task);
                task.setAssignedAt(task.getAssignedAt() == null ? now : task.getAssignedAt());
                task.setRejectionReason("Cancelled by admin");
                task.getHarvest().setStatus(Harvest.Status.AVAILABLE);
                task.getDemand().setStatus(Demand.Status.OPEN);
                harvestRepository.save(task.getHarvest());
                demandRepository.save(task.getDemand());
            }
        }
    }

    private void resetTaskTimeline(DeliveryTask task) {
        task.setAcceptedAt(null);
        task.setPickedUpAt(null);
        task.setInTransitAt(null);
        task.setDeliveredAt(null);
    }

    private void setTaskReservationState(DeliveryTask task) {
        task.getHarvest().setStatus(Harvest.Status.RESERVED);
        task.getDemand().setStatus(Demand.Status.RESERVED);
        harvestRepository.save(task.getHarvest());
        demandRepository.save(task.getDemand());
    }

    private User getUserWithRole(Long userId, Role role, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }

        return user;
    }

    private User findLeastLoadedAgent() {
        return findLeastLoadedAgent(null);
    }

    /**
     * Finds the agent with the fewest active tasks. Ties are broken by total task count,
     * then by agent ID. Optionally excludes one agent (used during reassignment to avoid
     * re-assigning to the same person).
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 if no eligible agents exist
     */
    private User findLeastLoadedAgent(Long excludedAgentId) {
        return userRepository.findByRole(Role.AGENT).stream()
                .filter(agent -> excludedAgentId == null || !agent.getId().equals(excludedAgentId))
                .min(Comparator
                        .comparingLong((User agent) -> deliveryTaskRepository.countByAssignedAgentIdAndStatusIn(agent.getId(), ACTIVE_TASK_STATUSES))
                        .thenComparingLong(agent -> deliveryTaskRepository.countByAssignedAgentId(agent.getId()))
                        .thenComparingLong(User::getId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No agent available for assignment"));
    }

    private void ensureNoActiveAssignmentConflicts(Long harvestId, Long demandId) {
        if (deliveryTaskRepository.existsByHarvestIdAndStatusIn(harvestId, ACTIVE_TASK_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Harvest already has an active assignment");
        }
        if (deliveryTaskRepository.existsByDemandIdAndStatusIn(demandId, ACTIVE_TASK_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Demand already has an active assignment");
        }
        if (deliveryTaskRepository.existsByHarvestIdAndDemandIdAndStatusIn(harvestId, demandId, ACTIVE_TASK_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This harvest-demand pair already has an active assignment");
        }
    }

    public boolean hasActiveTaskForHarvest(Long harvestId) {
        return deliveryTaskRepository.existsByHarvestIdAndStatusIn(harvestId, ACTIVE_TASK_STATUSES);
    }

    public boolean hasActiveTaskForDemand(Long demandId) {
        return deliveryTaskRepository.existsByDemandIdAndStatusIn(demandId, ACTIVE_TASK_STATUSES);
    }

    private Optional<TaskExceptionResponse> toTaskException(DeliveryTask task, LocalDateTime now) {
        if (task.getDemand().getRequestedQuantity() != null
                || task.getDemand().getRequestedRequiredDate() != null
                || task.getDemand().getRequestedTargetPrice() != null) {
            return Optional.of(TaskExceptionResponse.builder()
                    .taskId(task.getId())
                    .exceptionType("RETAILER_DEMAND_CHANGE_REQUEST")
                    .reason(task.getDemand().getChangeRequestReason() == null || task.getDemand().getChangeRequestReason().isBlank()
                            ? "Retailer requested a change to the reserved demand"
                            : task.getDemand().getChangeRequestReason())
                    .ageHours(calculateAgeHours(task.getAssignedAt(), now))
                    .task(task)
                    .build());
        }

        if (task.getHarvest().getStatus() == Harvest.Status.WITHDRAWAL_REQUESTED) {
            return Optional.of(TaskExceptionResponse.builder()
                    .taskId(task.getId())
                    .exceptionType("FARMER_WITHDRAWAL_REQUEST")
                    .reason(task.getRejectionReason() == null || task.getRejectionReason().isBlank() ? "Farmer requested withdrawal of reserved harvest" : task.getRejectionReason())
                    .ageHours(calculateAgeHours(task.getAssignedAt(), now))
                    .task(task)
                    .build());
        }

        if (task.getStatus() == DeliveryTask.Status.REJECTED) {
            return Optional.of(TaskExceptionResponse.builder()
                    .taskId(task.getId())
                    .exceptionType("AGENT_REJECTED")
                    .reason(task.getRejectionReason() == null || task.getRejectionReason().isBlank() ? "Agent rejected task" : task.getRejectionReason())
                    .ageHours(calculateAgeHours(task.getAssignedAt(), now))
                    .task(task)
                    .build());
        }

        if (ACTIVE_TASK_STATUSES.contains(task.getStatus()) && task.getAssignedAt() != null
                && ChronoUnit.HOURS.between(task.getAssignedAt(), now) >= 24) {
            return Optional.of(TaskExceptionResponse.builder()
                    .taskId(task.getId())
                    .exceptionType("DELIVERY_STUCK")
                    .reason("Task has been active for more than 24 hours")
                    .ageHours(calculateAgeHours(task.getAssignedAt(), now))
                    .task(task)
                    .build());
        }

        return Optional.empty();
    }

    private long calculateAgeHours(LocalDateTime assignedAt, LocalDateTime now) {
        if (assignedAt == null) {
            return 0;
        }
        return Math.max(ChronoUnit.HOURS.between(assignedAt, now), 0);
    }
}
