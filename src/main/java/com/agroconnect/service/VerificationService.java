package com.agroconnect.service;

import com.agroconnect.model.User;
import com.agroconnect.model.Verification;
import com.agroconnect.model.Verification.Status;
import com.agroconnect.model.Verification.Type;
import com.agroconnect.repository.VerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final VerificationRepository verificationRepository;

    public Verification createPhoneVerification(User user, String phone) {
        Verification verification = Verification.builder()
                .user(user)
                .type(Type.PHONE)
                .status(Status.PENDING)
                .data(phone)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return verificationRepository.save(verification);
    }

    // Add methods for document and business verification as needed
}
