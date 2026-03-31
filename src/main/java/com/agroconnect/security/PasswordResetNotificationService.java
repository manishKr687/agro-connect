package com.agroconnect.security;

import com.agroconnect.model.enums.PasswordResetChannel;

public interface PasswordResetNotificationService {
    void sendPasswordReset(PasswordResetChannel channel, String destination, String secret, String resetLink, long expiresInSeconds);
}
