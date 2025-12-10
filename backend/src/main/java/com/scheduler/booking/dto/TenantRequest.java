package com.scheduler.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class TenantRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Slug is required")
    private String slug;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String phone;
    private String logoUrl;
    private String description;
    private Map<String, String> brandColors;
    private String subscriptionTier;
    private String timezone; // IANA timezone identifier (e.g., "Europe/Amsterdam", "America/New_York")

    // Business owner details (for creating Clerk user)
    private String ownerFirstName;
    private String ownerLastName;
    private String ownerEmail;
    private String ownerPassword;
}
