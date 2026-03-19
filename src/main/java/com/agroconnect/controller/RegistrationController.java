package com.agroconnect.controller;

import com.agroconnect.model.User;
import com.agroconnect.repository.UserRepository;
import com.agroconnect.service.VerificationService;
import com.agroconnect.service.ApprovalService;
import com.agroconnect.dto.FarmerRegistrationRequest;
import com.agroconnect.dto.MediatorRegistrationRequest;
import com.agroconnect.dto.RetailerRegistrationRequest;
import com.agroconnect.model.enums.Role;
import com.agroconnect.model.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/register")
@RequiredArgsConstructor
public class RegistrationController {
        @PostMapping("/admin")
        public ResponseEntity<?> registerAdmin(@RequestBody com.agroconnect.dto.AdminRegistrationRequest request) {
            // Check for duplicate phone
            if (userRepository.findByPhone(request.getPhone()).isPresent()) {
                return ResponseEntity.badRequest().body("Phone already registered");
            }
            String hashedPassword = passwordEncoder.encode(request.getPasswordHash());
            User user = User.builder()
                .name(request.getName())
                .phone(request.getPhone())
                .passwordHash(hashedPassword)
                .role(Role.ADMIN)
                .status(Status.APPROVED)
                .build();
            userRepository.save(user);
            // Optionally, trigger verification or approval for admin if needed
            return ResponseEntity.ok("Admin registration successful");
        }
    private final UserRepository userRepository;
    private final VerificationService verificationService;
    private final ApprovalService approvalService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/farmer")
    public ResponseEntity<?> registerFarmer(@RequestBody FarmerRegistrationRequest request) {
        // Check for duplicate phone
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            return ResponseEntity.badRequest().body("Phone already registered");
        }
        String hashedPassword = passwordEncoder.encode(request.getPasswordHash());
        User user = User.builder()
            .name(request.getName())
            .phone(request.getPhone())
            .passwordHash(hashedPassword)
            .role(Role.FARMER)
            .status(Status.PENDING)
            .agentId(request.getAgentId())
            .build();
        userRepository.save(user);
        verificationService.createPhoneVerification(user, user.getPhone());
        approvalService.createApproval(user);
        // TODO: Trigger phone OTP send
        return ResponseEntity.ok("Farmer registration submitted");
    }

    @PostMapping("/mediator")
    public ResponseEntity<?> registerMediator(@RequestBody MediatorRegistrationRequest request) {
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            return ResponseEntity.badRequest().body("Phone already registered");
        }
        String hashedPassword = passwordEncoder.encode(request.getPasswordHash());
        User user = User.builder()
            .name(request.getName())
            .phone(request.getPhone())
            .passwordHash(hashedPassword)
            .role(Role.MEDIATOR)
            .status(Status.PENDING)
            .build();
        userRepository.save(user);
        verificationService.createPhoneVerification(user, user.getPhone());
        approvalService.createApproval(user);
        // TODO: Handle document upload, verification
        return ResponseEntity.ok("Mediator registration submitted");
    }

    @PostMapping("/retailer")
    public ResponseEntity<?> registerRetailer(@RequestBody RetailerRegistrationRequest request) {
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            return ResponseEntity.badRequest().body("Phone already registered");
        }
        String hashedPassword = passwordEncoder.encode(request.getPasswordHash());
        User user = User.builder()
            .name(request.getName())
            .phone(request.getPhone())
            .passwordHash(hashedPassword)
            .role(Role.RETAILER)
            .status(Status.PENDING)
            .businessName(request.getBusinessName())
            .build();
        userRepository.save(user);
        verificationService.createPhoneVerification(user, user.getPhone());
        approvalService.createApproval(user);
        // TODO: Handle business document upload, verification
        return ResponseEntity.ok("Retailer registration submitted");
    }

    // DTOs moved to com.agroconnect.dto package
}