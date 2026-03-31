package com.agroconnect.dto;

import com.agroconnect.model.enums.PasswordResetChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ForgotPasswordRequest {
    @NotNull
    private PasswordResetChannel channel;

    @NotBlank
    private String identifier;
}
