package com.agroconnect.dto;

import com.agroconnect.model.enums.PasswordResetChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotNull
    private PasswordResetChannel channel;

    @NotBlank
    private String identifier;

    private String token;

    private String otp;

    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;
}
