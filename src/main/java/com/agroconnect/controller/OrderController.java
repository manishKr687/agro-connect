package com.agroconnect.controller;

import com.agroconnect.model.*;
import com.agroconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private HarvestRepository harvestRepository;
    @Autowired
    private DemandRepository demandRepository;
    @Autowired
    private UserRepository userRepository;

    // Admin matches a harvest and demand, normalizes price, creates order
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create")
    public Order createOrder(@RequestParam Long harvestId,
                            @RequestParam Long demandId,
                            @RequestParam Long adminId,
                            @RequestParam Double normalizedPrice,
                            @RequestParam Long mediatorId) {
        Harvest harvest = harvestRepository.findById(harvestId).orElseThrow();
        Demand demand = demandRepository.findById(demandId).orElseThrow();
        User admin = userRepository.findById(adminId).orElseThrow();
        User mediator = userRepository.findById(mediatorId).orElseThrow();

        harvest.setStatus(Harvest.Status.MATCHED);
        demand.setStatus(Demand.Status.MATCHED);
        harvestRepository.save(harvest);
        demandRepository.save(demand);

        Order order = Order.builder()
                .harvest(harvest)
                .demand(demand)
                .admin(admin)
                .mediator(mediator)
                .normalizedPrice(normalizedPrice)
                .status(Order.Status.CREATED)
                .createdAt(LocalDateTime.now())
                .build();
        return orderRepository.save(order);
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
