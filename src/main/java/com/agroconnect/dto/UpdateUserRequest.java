package com.agroconnect.dto;

import com.agroconnect.model.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username may only contain letters, digits and underscores")
    private String username;

    @NotNull
    private Role role;

    @Size(min = 8, max = 100)
    private String password;
}
