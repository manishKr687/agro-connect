package com.agroconnect.service;

import com.agroconnect.dto.CancelTaskRequest;
import com.agroconnect.dto.CreateDeliveryTaskRequest;
import com.agroconnect.model.DeliveryTask;
import com.agroconnect.model.Demand;
import com.agroconnect.model.Harvest;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.DeliveryTaskRepository;
import com.agroconnect.repository.DemandRepository;
import com.agroconnect.repository.HarvestRepository;
import com.agroconnect.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryTaskServiceTest {

    @Mock DeliveryTaskRepository deliveryTaskRepository;
    @Mock HarvestRepository harvestRepository;
    @Mock DemandRepository demandRepository;
    @Mock UserRepository userRepository;
    @Mock AccessControlService accessControlService;

    @InjectMocks DeliveryTaskService deliveryTaskService;

    private User admin;
    private User agent;
    private Harvest availableHarvest;
    private Demand openDemand;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(1L).username("admin").role(Role.ADMIN).build();
        agent = User.builder().id(2L).username("agent1").role(Role.AGENT).build();

        availableHarvest = Harvest.builder()
                .id(10L).cropName("Tomato").quantity(100.0)
                .harvestDate(LocalDate.now()).expectedPrice(50.0)
                .status(Harvest.Status.AVAILABLE).build();

        openDemand = Demand.builder()
                .id(20L).cropName("Tomato").quantity(80.0)
                .requiredDate(LocalDate.now().plusDays(2)).targetPrice(55.0)
                .status(Demand.Status.OPEN).build();
    }

    private CreateDeliveryTaskRequest createRequest() {
        CreateDeliveryTaskRequest request = new CreateDeliveryTaskRequest();
        request.setAdminId(admin.getId());
        request.setAgentId(agent.getId());
        request.setHarvestId(availableHarvest.getId());
        request.setDemandId(openDemand.getId());
        return request;
    }

    // ── createTask ────────────────────────────────────────────────────────────

    @Test
    void createTask_validInputs_harvestAndDemandMoveToReserved() {
        when(accessControlService.requireAdmin(admin.getId())).thenReturn(admin);
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(harvestRepository.findById(availableHarvest.getId())).thenReturn(Optional.of(availableHarvest));
        when(demandRepository.findById(openDemand.getId())).thenReturn(Optional.of(openDemand));
        when(deliveryTaskRepository.existsByHarvestIdAndStatusIn(any(), any())).thenReturn(false);
        when(deliveryTaskRepository.existsByDemandIdAndStatusIn(any(), any())).thenReturn(false);
        when(deliveryTaskRepository.existsByHarvestIdAndDemandIdAndStatusIn(any(), any(), any())).thenReturn(false);
        when(harvestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeliveryTask task = deliveryTaskService.createTask(createRequest());

        assertThat(task.getStatus()).isEqualTo(DeliveryTask.Status.ASSIGNED);
        assertThat(availableHarvest.getStatus()).isEqualTo(Harvest.Status.RESERVED);
        assertThat(openDemand.getStatus()).isEqualTo(Demand.Status.RESERVED);
    }

    @Test
    void createTask_harvestNotAvailable_throwsBadRequest() {
        availableHarvest.setStatus(Harvest.Status.RESERVED);

        when(accessControlService.requireAdmin(admin.getId())).thenReturn(admin);
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(harvestRepository.findById(availableHarvest.getId())).thenReturn(Optional.of(availableHarvest));
        when(demandRepository.findById(openDemand.getId())).thenReturn(Optional.of(openDemand));

        assertThatThrownBy(() -> deliveryTaskService.createTask(createRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createTask_demandNotOpen_throwsBadRequest() {
        openDemand.setStatus(Demand.Status.RESERVED);

        when(accessControlService.requireAdmin(admin.getId())).thenReturn(admin);
        when(userRepository.findById(agent.getId())).thenReturn(Optional.of(agent));
        when(harvestRepository.findById(availableHarvest.getId())).thenReturn(Optional.of(availableHarvest));
        when(demandRepository.findById(openDemand.getId())).thenReturn(Optional.of(openDemand));

        assertThatThrownBy(() -> deliveryTaskService.createTask(createRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── acceptTask ────────────────────────────────────────────────────────────

    @Test
    void acceptTask_assignedTask_statusBecomesAccepted() {
        DeliveryTask task = DeliveryTask.builder()
                .id(100L).assignedAgent(agent)
                .harvest(availableHarvest).demand(openDemand)
                .status(DeliveryTask.Status.ASSIGNED).build();

        when(accessControlService.requireCurrentUser(agent.getId(), Role.AGENT)).thenReturn(agent);
        when(deliveryTaskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(deliveryTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeliveryTask result = deliveryTaskService.acceptTask(agent.getId(), 100L);

        assertThat(result.getStatus()).isEqualTo(DeliveryTask.Status.ACCEPTED);
        assertThat(result.getAcceptedAt()).isNotNull();
    }

    // ── rejectTask ────────────────────────────────────────────────────────────

    @Test
    void rejectTask_assignedTask_revertsHarvestAndDemand() {
        availableHarvest.setStatus(Harvest.Status.RESERVED);
        openDemand.setStatus(Demand.Status.RESERVED);

        DeliveryTask task = DeliveryTask.builder()
                .id(100L).assignedAgent(agent)
                .harvest(availableHarvest).demand(openDemand)
                .status(DeliveryTask.Status.ASSIGNED).build();

        when(accessControlService.requireCurrentUser(agent.getId(), Role.AGENT)).thenReturn(agent);
        when(deliveryTaskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(harvestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeliveryTask result = deliveryTaskService.rejectTask(agent.getId(), 100L, "Cannot reach location");

        assertThat(result.getStatus()).isEqualTo(DeliveryTask.Status.REJECTED);
        assertThat(availableHarvest.getStatus()).isEqualTo(Harvest.Status.AVAILABLE);
        assertThat(openDemand.getStatus()).isEqualTo(Demand.Status.OPEN);
    }

    // ── markDelivered ─────────────────────────────────────────────────────────

    @Test
    void markDelivered_inTransitTask_harvestSoldAndDemandFulfilled() {
        availableHarvest.setStatus(Harvest.Status.RESERVED);
        openDemand.setStatus(Demand.Status.RESERVED);

        DeliveryTask task = DeliveryTask.builder()
                .id(100L).assignedAgent(agent)
                .harvest(availableHarvest).demand(openDemand)
                .status(DeliveryTask.Status.IN_TRANSIT).build();

        when(accessControlService.requireCurrentUser(agent.getId(), Role.AGENT)).thenReturn(agent);
        when(deliveryTaskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(harvestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeliveryTask result = deliveryTaskService.markDelivered(agent.getId(), 100L);

        assertThat(result.getStatus()).isEqualTo(DeliveryTask.Status.DELIVERED);
        assertThat(availableHarvest.getStatus()).isEqualTo(Harvest.Status.SOLD);
        assertThat(openDemand.getStatus()).isEqualTo(Demand.Status.FULFILLED);
        assertThat(result.getDeliveredAt()).isNotNull();
    }

    // ── cancelTask ────────────────────────────────────────────────────────────

    @Test
    void cancelTask_activeTask_revertsHarvestAndDemand() {
        availableHarvest.setStatus(Harvest.Status.RESERVED);
        openDemand.setStatus(Demand.Status.RESERVED);

        DeliveryTask task = DeliveryTask.builder()
                .id(100L).assignedAgent(agent)
                .harvest(availableHarvest).demand(openDemand)
                .status(DeliveryTask.Status.ASSIGNED).build();

        CancelTaskRequest request = new CancelTaskRequest();
        request.setReason("Admin override");

        when(accessControlService.requireAdmin(admin.getId())).thenReturn(admin);
        when(deliveryTaskRepository.findById(100L)).thenReturn(Optional.of(task));
        when(harvestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(demandRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeliveryTask result = deliveryTaskService.cancelTaskForAdmin(admin.getId(), 100L, request);

        assertThat(result.getStatus()).isEqualTo(DeliveryTask.Status.CANCELLED);
        assertThat(availableHarvest.getStatus()).isEqualTo(Harvest.Status.AVAILABLE);
        assertThat(openDemand.getStatus()).isEqualTo(Demand.Status.OPEN);
    }

    @Test
    void cancelTask_deliveredTask_throwsBadRequest() {
        DeliveryTask task = DeliveryTask.builder()
                .id(100L).assignedAgent(agent)
                .harvest(availableHarvest).demand(openDemand)
                .status(DeliveryTask.Status.DELIVERED).build();

        CancelTaskRequest request = new CancelTaskRequest();
        request.setReason("Too late");

        when(accessControlService.requireAdmin(admin.getId())).thenReturn(admin);
        when(deliveryTaskRepository.findById(100L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> deliveryTaskService.cancelTaskForAdmin(admin.getId(), 100L, request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
