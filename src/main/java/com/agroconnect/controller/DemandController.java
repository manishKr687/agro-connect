package com.agroconnect.controller;

import com.agroconnect.model.Demand;
import com.agroconnect.repository.DemandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/demands")
public class DemandController {
    @Autowired
    private DemandRepository demandRepository;


    @PreAuthorize("hasRole('RETAILER')")
    @PostMapping
    public Demand addDemand(@RequestBody Demand demand) {
        return demandRepository.save(demand);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MEDIATOR')")
    @GetMapping
    public List<Demand> getAllDemands() {
        return demandRepository.findAll();
    }
}
