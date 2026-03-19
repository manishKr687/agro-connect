package com.agroconnect.dto;

import lombok.Data;

@Data
public class MediatorRegistrationRequest {
    private String name;
    private String phone;
    private String passwordHash;
    // Add document fields as needed
}
