package com.scheduler.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookingRequest {
    @NotNull(message = "Session type is required")
    private UUID sessionTypeId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    private Integer participants = 1;
    private String notes;
    private String customerTimezone;

    // For customer booking (if not logged in)
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
}
