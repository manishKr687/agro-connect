package com.agroconnect.dto;

import lombok.Data;

@Data
public class AdminRegistrationRequest {
    private String name;
    private String phone;
    private String passwordHash;
}
