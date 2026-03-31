package com.agroconnect.dto;

import com.agroconnect.model.enums.PasswordResetChannel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ForgotPasswordResponse {
    String message;
    PasswordResetChannel channel;
    String maskedDestination;
    Long expiresInSeconds;
    String previewToken;
    String previewOtp;
    String previewResetLink;
}
