package com.agroconnect.service;

import com.agroconnect.dto.DemandRequest;
import com.agroconnect.model.Demand;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.DemandRepository;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandService {
    private final DemandRepository demandRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;

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

        demandRepository.delete(demand);
    }

    public List<Demand> getAllDemandsForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);
        return demandRepository.findAll();
    }

    public Demand updateDemandForAdmin(Long adminId, Long demandId, DemandRequest request) {
        accessControlService.requireAdmin(adminId);
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found"));

        demand.setCropName(request.getCropName());
        demand.setQuantity(request.getQuantity());
        demand.setRequiredDate(request.getRequiredDate());
        demand.setTargetPrice(request.getTargetPrice());
        return demandRepository.save(demand);
    }

    public void deleteDemandForAdmin(Long adminId, Long demandId) {
        accessControlService.requireAdmin(adminId);
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demand not found"));
        demandRepository.delete(demand);
    }
}
