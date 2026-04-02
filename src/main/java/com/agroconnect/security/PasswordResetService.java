package com.agroconnect.security;

import com.agroconnect.dto.ForgotPasswordRequest;
import com.agroconnect.dto.ForgotPasswordResponse;
import com.agroconnect.dto.ResetPasswordRequest;
import com.agroconnect.model.PasswordResetChallenge;
import com.agroconnect.model.User;
import com.agroconnect.model.enums.PasswordResetChannel;
import com.agroconnect.repository.PasswordResetChallengeRepository;
import com.agroconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private static final String GENERIC_MESSAGE = "If that account exists, password reset instructions have been sent.";

    private final PasswordResetChallengeRepository passwordResetChallengeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetNotificationService passwordResetNotificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.email-expiration-ms:1800000}")
    private long emailExpirationMs;

    @Value("${app.password-reset.sms-expiration-ms:600000}")
    private long smsExpirationMs;

    @Value("${app.password-reset.debug-enabled:false}")
    private boolean debugEnabled;

    @Value("${app.password-reset.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Transactional
    public ForgotPasswordResponse requestReset(ForgotPasswordRequest request) {
        PasswordResetChannel channel = request.getChannel();
        String identifier = normalizeIdentifier(channel, request.getIdentifier());
        Optional<User> user = findUser(channel, identifier);

        if (user.isEmpty()) {
            return genericResponse(channel);
        }

        passwordResetChallengeRepository.deleteByUserIdAndChannelAndUsedAtIsNull(user.get().getId(), channel);

        String secret = generateSecret(channel);
        long expirationMs = channel == PasswordResetChannel.EMAIL ? emailExpirationMs : smsExpirationMs;
        long expiresInSeconds = expirationMs / 1000;
        String resetLink = channel == PasswordResetChannel.EMAIL
                ? "%s/reset-password?channel=%s&identifier=%s&token=%s".formatted(
                        frontendBaseUrl,
                        channel.name(),
                        encodeUrlComponent(identifier),
                        encodeUrlComponent(secret))
                : null;

        passwordResetChallengeRepository.save(PasswordResetChallenge.builder()
                .user(user.get())
                .channel(channel)
                .identifier(identifier)
                .secretHash(hashSecret(secret))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusMillis(expirationMs))
                .build());

        passwordResetNotificationService.sendPasswordReset(channel, identifier, secret, resetLink, expiresInSeconds);

        return ForgotPasswordResponse.builder()
                .message(GENERIC_MESSAGE)
                .channel(channel)
                .maskedDestination(maskIdentifier(channel, identifier))
                .expiresInSeconds(expiresInSeconds)
                .previewToken(debugEnabled && channel == PasswordResetChannel.EMAIL ? secret : null)
                .previewOtp(debugEnabled && channel == PasswordResetChannel.SMS ? secret : null)
                .previewResetLink(debugEnabled && channel == PasswordResetChannel.EMAIL ? resetLink : null)
                .build();
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetChannel channel = request.getChannel();
        String identifier = normalizeIdentifier(channel, request.getIdentifier());
        String providedSecret = extractCredential(channel, request);
        List<PasswordResetChallenge> challenges = passwordResetChallengeRepository
                .findByChannelAndIdentifierAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(channel, identifier, Instant.now());

        PasswordResetChallenge challenge = challenges.stream()
                .filter(candidate -> candidate.getSecretHash().equals(hashSecret(providedSecret)))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reset token or OTP is invalid or expired"));

        User user = challenge.getUser();
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        challenge.setUsedAt(Instant.now());
        passwordResetChallengeRepository.save(challenge);
        tokenBlacklistService.revokeUser(user.getPhoneNumber());
        refreshTokenService.revokeUserSessions(user.getPhoneNumber());
    }

    @Transactional
    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        passwordResetChallengeRepository.deleteByExpiresAtBefore(Instant.now());
    }

    private Optional<User> findUser(PasswordResetChannel channel, String identifier) {
        return channel == PasswordResetChannel.EMAIL
                ? userRepository.findByEmail(identifier)
                : userRepository.findByPhoneNumber(identifier);
    }

    private ForgotPasswordResponse genericResponse(PasswordResetChannel channel) {
        return ForgotPasswordResponse.builder()
                .message(GENERIC_MESSAGE)
                .channel(channel)
                .build();
    }

    private String extractCredential(PasswordResetChannel channel, ResetPasswordRequest request) {
        String candidate = channel == PasswordResetChannel.EMAIL ? request.getToken() : request.getOtp();
        if (candidate == null || candidate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    channel == PasswordResetChannel.EMAIL ? "Reset token is required" : "OTP is required");
        }
        return candidate.trim();
    }

    private String generateSecret(PasswordResetChannel channel) {
        if (channel == PasswordResetChannel.SMS) {
            return "%06d".formatted(secureRandom.nextInt(1_000_000));
        }

        byte[] buffer = new byte[24];
        secureRandom.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private String hashSecret(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(secret.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String normalizeIdentifier(PasswordResetChannel channel, String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifier is required");
        }

        String trimmed = identifier.trim();
        if (channel == PasswordResetChannel.EMAIL) {
            return trimmed.toLowerCase();
        }

        return trimmed.replaceAll("[\\s()\\-]", "");
    }

    private String maskIdentifier(PasswordResetChannel channel, String identifier) {
        if (channel == PasswordResetChannel.EMAIL) {
            int atIndex = identifier.indexOf('@');
            if (atIndex <= 1) {
                return identifier;
            }
            return identifier.charAt(0) + "***" + identifier.substring(atIndex);
        }

        if (identifier.length() <= 4) {
            return identifier;
        }
        return "*".repeat(Math.max(0, identifier.length() - 4)) + identifier.substring(identifier.length() - 4);
    }

    private String encodeUrlComponent(String value) {
        return value.replace("+", "%2B").replace("@", "%40");
    }
}
