package com.agroconnect.service;

import com.agroconnect.dto.HarvestRequest;
import com.agroconnect.model.Harvest;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.Role;
import com.agroconnect.repository.HarvestRepository;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HarvestService {
    private final HarvestRepository harvestRepository;
    private final UserRepository userRepository;
    private final AccessControlService accessControlService;

    public Harvest createHarvest(Long farmerId, HarvestRequest request) {
        User farmer = accessControlService.requireCurrentUser(farmerId, Role.FARMER);

        Harvest harvest = Harvest.builder()
                .farmer(farmer)
                .cropName(request.getCropName())
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
        Harvest harvest = harvestRepository.findById(harvestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Harvest not found"));

        if (!harvest.getFarmer().getId().equals(farmer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Harvest does not belong to this farmer");
        }

        harvest.setCropName(request.getCropName());
        harvest.setQuantity(request.getQuantity());
        harvest.setHarvestDate(request.getHarvestDate());
        harvest.setExpectedPrice(request.getExpectedPrice());
        return harvestRepository.save(harvest);
    }

    public void deleteHarvest(Long farmerId, Long harvestId) {
        User farmer = accessControlService.requireCurrentUser(farmerId, Role.FARMER);
        Harvest harvest = harvestRepository.findById(harvestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Harvest not found"));

        if (!harvest.getFarmer().getId().equals(farmer.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Harvest does not belong to this farmer");
        }

        harvestRepository.delete(harvest);
    }

    public List<Harvest> getAllHarvestsForAdmin(Long adminId) {
        accessControlService.requireAdmin(adminId);
        return harvestRepository.findAll();
    }

    public Harvest updateHarvestForAdmin(Long adminId, Long harvestId, HarvestRequest request) {
        accessControlService.requireAdmin(adminId);
        Harvest harvest = harvestRepository.findById(harvestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Harvest not found"));

        harvest.setCropName(request.getCropName());
        harvest.setQuantity(request.getQuantity());
        harvest.setHarvestDate(request.getHarvestDate());
        harvest.setExpectedPrice(request.getExpectedPrice());
        return harvestRepository.save(harvest);
    }

    public void deleteHarvestForAdmin(Long adminId, Long harvestId) {
        accessControlService.requireAdmin(adminId);
        Harvest harvest = harvestRepository.findById(harvestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Harvest not found"));
        harvestRepository.delete(harvest);
    }
}
