package com.agroconnect.controller;

import com.agroconnect.model.*;
import com.agroconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

// ...existing code...
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderRepository orderRepository;
        @Autowired
        private com.agroconnect.admin.AdminService adminService;

        @PreAuthorize("hasRole('ADMIN')")
        @PostMapping("/create")
        public Order createOrder(@RequestParam Long harvestId,
                                @RequestParam Long demandId,
                                @RequestParam Long adminId,
                                @RequestParam Double normalizedPrice,
                                @RequestParam Long mediatorId) {
            return adminService.createOrder(harvestId, demandId, adminId, normalizedPrice, mediatorId);
        }
    // ...existing code...

    // ...admin-specific order creation moved to AdminController...

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}
