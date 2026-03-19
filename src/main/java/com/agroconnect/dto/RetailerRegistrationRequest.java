package com.agroconnect.dto;

import lombok.Data;

@Data
public class RetailerRegistrationRequest {
    private String name;
    private String phone;
    private String passwordHash;
    private String businessName;
    // Add business document fields as needed
}
