package com.agroconnect.dto;

import com.agroconnect.model.enums.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private Long userId;
    private String username;
    private Role role;
    private String token;
}
