package com.agroconnect.controller;

import com.agroconnect.model.Demand;
import com.agroconnect.repository.DemandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/demands")
public class DemandController {
    @Autowired
    private DemandRepository demandRepository;

    @PostMapping
    public Demand addDemand(@RequestBody Demand demand) {
        return demandRepository.save(demand);
    }

    @GetMapping
    public List<Demand> getAllDemands() {
        return demandRepository.findAll();
    }
}
