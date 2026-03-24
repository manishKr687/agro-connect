package com.agroconnect.service;

import com.agroconnect.dto.DemandChangeRequest;
import com.agroconnect.dto.DemandRequest;
import com.agroconnect.model.DeliveryTask;
import com.agroconnect.dto.UpdateDemandStatusRequest;
import com.agroconnect.model.Demand;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.DeliveryTaskRepository;
import com.agroconnect.repository.DemandRepository;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Business logic for retailer demand management and admin demand oversight.
 *
 * <p>Retailers can create, update, and delete their own {@code OPEN} demands.
 * Once a demand is {@code RESERVED} (linked to a delivery task), the retailer can only
 * submit a change request via {@link #requestDemandChange}. The pending changes are
 * stored in the {@code requested*} fields on the demand and must be approved or rejected
 * by an admin before taking effect.
 *
 * <p>Admin operations bypass ownership checks and can update a demand's status directly,
 * provided no active delivery task exists for it.
 */
@Service
@RequiredArgsConstructor
public class DemandService {
    /** Task statuses that indicate an active in-progress delivery. */
    private static final List<DeliveryTask.Status> ACTIVE_TASK_STATUSES = List.of(
            DeliveryTask.Status.ASSIGNED,
            DeliveryTask.Status.ACCEPTED,
            DeliveryTask.Status.PICKED_UP,
            DeliveryTask.Status.IN_TRANSIT
    );

    private final DemandRepository demandRepository;
    private final DeliveryTaskRepository deliveryTaskRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;
    private final DeliveryTaskService deliveryTaskService;

    public Demand createDemand(Long retailerId, DemandRequest request) {
        User retailer = accessControlService.requireCurrentUser(retailerId, Role.RETAILER);

        Demand demand = Demand.builder()
                .retailer(retailer)
                .cropName(request.getCropName())
                .quantity(request.getQuantity())
                .requiredDate(request.getRequiredDate())
                .targetPrice(request.getTargetPrice())
                .status(Demand.Status.OPEN)
                .build();

        return demandRepository.save(demand);
    }

    public List<Demand> getDemandsForRetailer(Long retailerId) {
        accessControlService.requireCurrentUser(retailerId, Role.RETAILER);
        return demandRepository.findByRetailerId(retailerId);
    }

    public Demand updateDemand(Long retailerId, Long demandId, DemandRequest request) {
        User retailer = accessControlService.requireCurrentUser(retailerId, Role.RETAILER);
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found"));

        if (!demand.getRetailer().getId().equals(retailer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demand does not belong to this retailer");
        }
        if (demand.getStatus() != Demand.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only open demands can be edited directly");
        }

        demand.setCropName(request.getCropName());
        demand.setQuantity(request.getQuantity());
        demand.setRequiredDate(request.getRequiredDate());
        demand.setTargetPrice(request.getTargetPrice());
        return demandRepository.save(demand);
    }

    public void deleteDemand(Long retailerId, Long demandId) {
        User retailer = accessControlService.requireCurrentUser(retailerId, Role.RETAILER);
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found"));

        if (!demand.getRetailer().getId().equals(retailer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demand does not belong to this retailer");
        }
        if (demand.getStatus() != Demand.Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only open demands can be deleted");
        }

        demandRepository.delete(demand);
    }

    /**
     * Submits a change request for a reserved demand. The requested values are stored in the
     * {@code requested*} fields on the demand entity and appear in the admin exception queue.
     *
     * <p>The actual demand values are not modified until an admin calls
     * {@link #approveDemandChangeForAdmin}. Calling {@link #rejectDemandChangeForAdmin} clears
     * the pending fields without applying any changes.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 if demand is not RESERVED
     * @throws org.springframework.web.server.ResponseStatusException 409 if no active task exists for the demand
     */
    public Demand requestDemandChange(Long retailerId, Long demandId, DemandChangeRequest request) {
        User retailer = accessControlService.requireCurrentUser(retailerId, Role.RETAILER);
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found"));

        if (!demand.getRetailer().getId().equals(retailer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Demand does not belong to this retailer");
        }
        if (demand.getStatus() != Demand.Status.RESERVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only reserved demands can request changes");
        }

        deliveryTaskRepository.findByDemandIdAndStatusIn(demandId, ACTIVE_TASK_STATUSES).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active task found for this reserved demand"));

        demand.setRequestedQuantity(request.getQuantity());
        demand.setRequestedRequiredDate(request.getRequiredDate());
        demand.setRequestedTargetPrice(request.getTargetPrice());
        demand.setChangeRequestReason(request.getReason().trim());
        return demandRepository.save(demand);
    }

    public List<Demand> getAllDemandsForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);
        return demandRepository.findAll();
    }

    public Demand updateDemandForAdmin(Long adminId, Long demandId, UpdateDemandStatusRequest request) {
        accessControlService.requireAdmin(adminId);
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found"));

        if (deliveryTaskService.hasActiveTaskForDemand(demandId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Demand status cannot be changed while it has an active task");
        }

        demand.setStatus(request.getStatus());
        return demandRepository.save(demand);
    }

    /**
     * Applies the pending change request to the demand's live fields and clears the {@code requested*} fields.
     *
     * <p>Validates that the requested quantity does not exceed the reserved harvest's available quantity
     * before applying, to prevent the match from becoming invalid.
     *
     * @throws org.springframework.web.server.ResponseStatusException 400 if no change request is pending
     * @throws org.springframework.web.server.ResponseStatusException 400 if the requested quantity exceeds harvest capacity
     */
    public Demand approveDemandChangeForAdmin(Long adminId, Long demandId) {
        accessControlService.requireAdmin(adminId);
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found"));

        if (demand.getRequestedQuantity() == null && demand.getRequestedRequiredDate() == null && demand.getRequestedTargetPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending demand change request found");
        }

        DeliveryTask task = deliveryTaskRepository.findByDemandIdAndStatusIn(demandId, ACTIVE_TASK_STATUSES).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No active task found for this demand"));

        if (demand.getRequestedQuantity() != null && task.getHarvest().getQuantity() < demand.getRequestedQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requested quantity exceeds the reserved harvest quantity. Cancel and rematch instead.");
        }

        demand.setQuantity(demand.getRequestedQuantity() != null ? demand.getRequestedQuantity() : demand.getQuantity());
        demand.setRequiredDate(demand.getRequestedRequiredDate() != null ? demand.getRequestedRequiredDate() : demand.getRequiredDate());
        demand.setTargetPrice(demand.getRequestedTargetPrice() != null ? demand.getRequestedTargetPrice() : demand.getTargetPrice());
        clearDemandChangeRequest(demand);
        return demandRepository.save(demand);
    }

    public Demand rejectDemandChangeForAdmin(Long adminId, Long demandId) {
        accessControlService.requireAdmin(adminId);
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found"));

        if (demand.getRequestedQuantity() == null && demand.getRequestedRequiredDate() == null && demand.getRequestedTargetPrice() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No pending demand change request found");
        }

        clearDemandChangeRequest(demand);
        return demandRepository.save(demand);
    }

    private void clearDemandChangeRequest(Demand demand) {
        demand.setRequestedQuantity(null);
        demand.setRequestedRequiredDate(null);
        demand.setRequestedTargetPrice(null);
        demand.setChangeRequestReason(null);
    }
}
