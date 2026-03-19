package com.agroconnect.controller;

import com.agroconnect.model.Harvest;
import com.agroconnect.repository.HarvestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/harvests")
public class HarvestController {
    @Autowired
    private HarvestRepository harvestRepository;


    @PreAuthorize("hasRole('FARMER')")
    @PostMapping
    public Harvest addHarvest(@RequestBody Harvest harvest) {
        return harvestRepository.save(harvest);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('FARMER') or hasRole('MEDIATOR') or hasRole('RETAILER')")
    @GetMapping
    public List<Harvest> getAllHarvests() {
        return harvestRepository.findAll();
    }
}
