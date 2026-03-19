package com.agroconnect.controller;

import com.agroconnect.model.Approval;
import com.agroconnect.model.User;
import com.agroconnect.repository.ApprovalRepository;
import com.agroconnect.repository.UserRepository;
import com.agroconnect.service.ApprovalService;
import com.agroconnect.model.enums.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/approvals")
@RequiredArgsConstructor
public class AdminApprovalController {
    private final ApprovalRepository approvalRepository;
    private final UserRepository userRepository;
    private final ApprovalService approvalService;

    // List all pending/under review applications
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<Approval> listApprovals(@RequestParam(required = false) String status) {
        if (status != null) {
            return approvalRepository.findByStatus(Approval.Status.valueOf(status));
        }
        return approvalRepository.findAll();
    }

    // Approve a user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{approvalId}/approve")
    public ResponseEntity<?> approve(@PathVariable Long approvalId, @RequestParam Long adminId) {
        Approval approval = approvalRepository.findById(approvalId).orElseThrow();
        User admin = userRepository.findById(adminId).orElseThrow();
        approvalService.updateApprovalStatus(approval, Approval.Status.APPROVED, admin, null);
        User user = approval.getUser();
        user.setStatus(Status.APPROVED);
        userRepository.save(user);
        return ResponseEntity.ok("User approved");
    }

    // Reject a user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{approvalId}/reject")
    public ResponseEntity<?> reject(@PathVariable Long approvalId, @RequestParam Long adminId, @RequestParam String reason) {
        Approval approval = approvalRepository.findById(approvalId).orElseThrow();
        User admin = userRepository.findById(adminId).orElseThrow();
        approvalService.updateApprovalStatus(approval, Approval.Status.REJECTED, admin, reason);
        User user = approval.getUser();
        user.setStatus(Status.REJECTED);
        userRepository.save(user);
        return ResponseEntity.ok("User rejected");
    }

    // Suspend a user
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{approvalId}/suspend")
    public ResponseEntity<?> suspend(@PathVariable Long approvalId, @RequestParam Long adminId, @RequestParam String reason) {
        Approval approval = approvalRepository.findById(approvalId).orElseThrow();
        User admin = userRepository.findById(adminId).orElseThrow();
        approvalService.updateApprovalStatus(approval, Approval.Status.SUSPENDED, admin, reason);
        User user = approval.getUser();
        user.setStatus(Status.SUSPENDED);
        userRepository.save(user);
        return ResponseEntity.ok("User suspended");
    }
}
