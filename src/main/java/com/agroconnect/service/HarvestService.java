package com.agroconnect.service;

import com.agroconnect.dto.HarvestRequest;
import com.agroconnect.dto.HarvestWithdrawalRequest;
import com.agroconnect.model.DeliveryTask;
import com.agroconnect.dto.UpdateHarvestStatusRequest;
import com.agroconnect.model.Harvest;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.DeliveryTaskRepository;
import com.agroconnect.repository.HarvestRepository;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business logic for farmer harvest management and admin harvest oversight.
 *
 * <p>Farmers can create, update, and delete their own {@code AVAILABLE} harvests.
 * Once a harvest is {@code RESERVED} (linked to a delivery task), the farmer can only
 * request a withdrawal — the admin must then cancel the task to release the harvest.
 *
 * <p>Admin operations bypass ownership checks and can update a harvest's status directly,
 * provided no active delivery task exists for it.
 */
@Service
@RequiredArgsConstructor
public class HarvestService {
    private static final List<DeliveryTask.Status> ACTIVE_TASK_STATUSES = DeliveryTask.ACTIVE_STATUSES;

    private final HarvestRepository harvestRepository;
    private final DeliveryTaskRepository deliveryTaskRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final DeliveryTaskService deliveryTaskService;
    private final CropNormalizerClient cropNormalizerClient;

    /**
     * Creates a new harvest for the given farmer. Status is set to {@code AVAILABLE} automatically.
     *
     * @throws org.springframework.web.server.ResponseStatusException 403 if the caller is not the specified farmer
     */
    public Harvest createHarvest(Long farmerId, HarvestRequest request) {
        User farmer = accessControlService.requireCurrentUser(farmerId, Role.FARMER);

        Harvest harvest = Harvest.builder()
                .farmer(farmer)
                .cropName(cropNormalizerClient.normalize(request.getCropName()))
                .quantity(request.getQuantity())
                .harvestDate(request.getHarvestDate())
                .expectedPrice(request.getExpectedPrice())
                .status(Harvest.Status.AVAILABLE)
                .build();

        return harvestRepository.save(harvest);
    }

    public List<Harvest> getHarvestsForFarmer(Long farmerId) {
        accessControlService.requireCurrentUser(farmerId, Role.FARMER);
        return harvestRepository.findByFarmerId(farmerId);
    }

    public Harvest updateHarvest(Long farmerId, Long harvestId, HarvestRequest request) {
        User farmer = accessControlService.requireCurrentUser(farmerId, Role.FARMER);
        Harvest harvest = requireOwnedHarvest(harvestId, farmer);

        if (harvest.getStatus() != Harvest.Status.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only available harvests can be edited");
        }

        harvest.setCropName(cropNormalizerClient.normalize(request.getCropName()));
        harvest.setQuantity(request.getQuantity());
        harvest.setHarvestDate(request.getHarvestDate());
        harvest.setExpectedPrice(request.getExpectedPrice());
        return harvestRepository.save(harvest);
    }

    public void deleteHarvest(Long farmerId, Long harvestId) {
        User farmer = accessControlService.requireCurrentUser(farmerId, Role.FARMER);
        Harvest harvest = requireOwnedHarvest(harvestId, farmer);

        if (harvest.getStatus() != Harvest.Status.AVAILABLE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only available harvests can be deleted");
        }

        harvestRepository.delete(harvest);
    }

    /**
     * Flags a reserved harvest for withdrawal. The harvest must currently be {@code RESERVED}
     * and have an active delivery task. The withdrawal reason is stored on the task's
     * {@code rejectionReason} field and surfaces in the admin exception queue.
     *
     * <p>The admin must cancel the task to complete the withdrawal.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 if harvest is not RESERVED
     * @throws org.springframework.web.server.ResponseStatusException 409 if no active task exists for the harvest
     */
    public Harvest requestWithdrawal(Long farmerId, Long harvestId, HarvestWithdrawalRequest request) {
        User farmer = accessControlService.requireCurrentUser(farmerId, Role.FARMER);
        Harvest harvest = requireOwnedHarvest(harvestId, farmer);
        if (harvest.getStatus() != Harvest.Status.RESERVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only reserved harvests can request withdrawal");
        }

        DeliveryTask activeTask = deliveryTaskRepository.findByHarvestIdAndStatusIn(harvestId, ACTIVE_TASK_STATUSES).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active task found for this reserved harvest"));

        harvest.setStatus(Harvest.Status.WITHDRAWAL_REQUESTED);
        activeTask.setRejectionReason(request.getReason().trim());
        deliveryTaskRepository.save(activeTask);
        return harvestRepository.save(harvest);
    }

    public List<Harvest> getAllHarvestsForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);
        return harvestRepository.findAll();
    }

    public Harvest updateHarvestForAdmin(Long adminId, Long harvestId, UpdateHarvestStatusRequest request) {
        accessControlService.requireAdmin(adminId);
        Harvest harvest = harvestRepository.findById(harvestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Harvest not found"));

        if (deliveryTaskService.hasActiveTaskForHarvest(harvestId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Harvest status cannot be changed while it has an active task");
        }

        harvest.setStatus(request.getStatus());
        return harvestRepository.save(harvest);
    }

    private Harvest requireOwnedHarvest(Long harvestId, User farmer) {
        Harvest harvest = harvestRepository.findById(harvestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Harvest not found"));
        if (!harvest.getFarmer().getId().equals(farmer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Harvest does not belong to this farmer");
        }
        return harvest;
    }
}
