package com.scheduler.booking.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TenantProfileRequest {
    private String logoUrl;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;
}
