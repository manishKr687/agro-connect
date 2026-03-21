package com.agroconnect.dto;

import com.agroconnect.model.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @NotBlank
    private String username;

    @NotNull
    private Role role;

    private String password;
}
