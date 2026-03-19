package com.agroconnect.dto;

import lombok.Data;

@Data
public class FarmerRegistrationRequest {
    private String name;
    private String phone;
    private String passwordHash;
    private Long agentId; // Optional
}
