package com.agroconnect.controller;

import com.agroconnect.model.User;
import com.agroconnect.service.AccessControlService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AccessControlService accessControlService;

    public UserController(AccessControlService accessControlService) {
        this.accessControlService = accessControlService;
    }

    @GetMapping("/me")
    public User me() {
        return accessControlService.getCurrentUser();
    }
}
