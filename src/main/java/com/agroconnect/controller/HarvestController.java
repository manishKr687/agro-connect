package com.agroconnect.controller;

import com.agroconnect.dto.HarvestRequest;
import com.agroconnect.dto.HarvestWithdrawalRequest;
import com.agroconnect.model.Harvest;
import com.agroconnect.service.HarvestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/farmers/{farmerId}/harvests")
@RequiredArgsConstructor
public class HarvestController {
    private final HarvestService harvestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Harvest createHarvest(
            @PathVariable Long farmerId,
            @Valid @RequestBody HarvestRequest request
    ) {
        return harvestService.createHarvest(farmerId, request);
    }

    @GetMapping
    public List<Harvest> getHarvests(@PathVariable Long farmerId) {
        return harvestService.getHarvestsForFarmer(farmerId);
    }

    @PutMapping("/{harvestId}")
    public Harvest updateHarvest(
            @PathVariable Long farmerId,
            @PathVariable Long harvestId,
            @Valid @RequestBody HarvestRequest request
    ) {
        return harvestService.updateHarvest(farmerId, harvestId, request);
    }

    @DeleteMapping("/{harvestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHarvest(@PathVariable Long farmerId, @PathVariable Long harvestId) {
        harvestService.deleteHarvest(farmerId, harvestId);
    }

    @PostMapping("/{harvestId}/withdrawal-request")
    public Harvest requestWithdrawal(
            @PathVariable Long farmerId,
            @PathVariable Long harvestId,
            @Valid @RequestBody HarvestWithdrawalRequest request
    ) {
        return harvestService.requestWithdrawal(farmerId, harvestId, request);
    }
}
