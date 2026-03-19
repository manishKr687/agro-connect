package com.agroconnect.service;

import com.agroconnect.model.User;
import com.agroconnect.model.Approval;
import com.agroconnect.model.Approval.Status;
import com.agroconnect.repository.ApprovalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ApprovalService {
    private final ApprovalRepository approvalRepository;

    public Approval createApproval(User user) {
        Approval approval = Approval.builder()
                .user(user)
                .status(Status.PENDING)
                .updatedAt(LocalDateTime.now())
                .build();
        return approvalRepository.save(approval);
    }

    public Approval updateApprovalStatus(Approval approval, Status status, User reviewer, String reason) {
        approval.setStatus(status);
        approval.setReviewer(reviewer);
        approval.setReason(reason);
        approval.setUpdatedAt(LocalDateTime.now());
        return approvalRepository.save(approval);
    }
}
