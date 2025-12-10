package com.scheduler.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;

@Data
public class BusinessHoursRequest {
    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    private String startTime; // Format: "HH:mm" (e.g., "09:00")

    @NotNull(message = "End time is required")
    private String endTime; // Format: "HH:mm" (e.g., "17:00")

    private boolean enabled = true;
}
