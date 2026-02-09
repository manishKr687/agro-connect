package com.agroconnect.controller;

import com.agroconnect.model.Order;
import com.agroconnect.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/mediator")
public class MediatorController {
    @Autowired
    private OrderRepository orderRepository;

    // Mediator updates collection and delivery status
    @PreAuthorize("hasRole('MEDIATOR')")
    @PostMapping("/collect/{orderId}")
    public Order collectOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(Order.Status.COLLECTED);
        order.setCollectedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @PreAuthorize("hasRole('MEDIATOR')")
    @PostMapping("/deliver/{orderId}")
    public Order deliverOrder(@PathVariable Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(Order.Status.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    @PreAuthorize("hasRole('MEDIATOR')")
    @GetMapping("/assigned/{mediatorId}")
    public List<Order> getAssignedOrders(@PathVariable Long mediatorId) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getMediator().getId().equals(mediatorId))
                .toList();
    }
}
