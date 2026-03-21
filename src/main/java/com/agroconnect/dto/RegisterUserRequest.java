package com.agroconnect.dto;

import com.agroconnect.model.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterUserRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotNull
    private Role role;
}
