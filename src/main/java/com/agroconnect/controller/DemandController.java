package com.agroconnect.controller;

import com.agroconnect.dto.DemandChangeRequest;
import com.agroconnect.dto.DemandRequest;
import com.agroconnect.model.Demand;
import com.agroconnect.service.DemandService;
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
@RequestMapping("/api/retailers/{retailerId}/demands")
@RequiredArgsConstructor
public class DemandController {
    private final DemandService demandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Demand createDemand(
            @PathVariable Long retailerId,
            @Valid @RequestBody DemandRequest request
    ) {
        return demandService.createDemand(retailerId, request);
    }

    @GetMapping
    public List<Demand> getDemands(@PathVariable Long retailerId) {
        return demandService.getDemandsForRetailer(retailerId);
    }

    @PutMapping("/{demandId}")
    public Demand updateDemand(
            @PathVariable Long retailerId,
            @PathVariable Long demandId,
            @Valid @RequestBody DemandRequest request
    ) {
        return demandService.updateDemand(retailerId, demandId, request);
    }

    @DeleteMapping("/{demandId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDemand(@PathVariable Long retailerId, @PathVariable Long demandId) {
        demandService.deleteDemand(retailerId, demandId);
    }

    @PostMapping("/{demandId}/change-request")
    public Demand requestDemandChange(
            @PathVariable Long retailerId,
            @PathVariable Long demandId,
            @Valid @RequestBody DemandChangeRequest request
    ) {
        return demandService.requestDemandChange(retailerId, demandId, request);
    }
}
