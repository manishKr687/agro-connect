package com.agroconnect.controller;

import com.agroconnect.model.Harvest;
import com.agroconnect.repository.HarvestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/harvests")
public class HarvestController {
    @Autowired
    private HarvestRepository harvestRepository;

    @PostMapping
    public Harvest addHarvest(@RequestBody Harvest harvest) {
        return harvestRepository.save(harvest);
    }

    @GetMapping
    public List<Harvest> getAllHarvests() {
        return harvestRepository.findAll();
    }
}
