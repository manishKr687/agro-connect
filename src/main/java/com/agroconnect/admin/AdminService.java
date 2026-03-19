package com.agroconnect.admin;


import com.agroconnect.model.*;
import com.agroconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AdminService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private HarvestRepository harvestRepository;
    @Autowired
    private DemandRepository demandRepository;
    @Autowired
    private UserRepository userRepository;

    public Order createOrder(Long harvestId, Long demandId, Long adminId, Double normalizedPrice, Long mediatorId) {
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
}
