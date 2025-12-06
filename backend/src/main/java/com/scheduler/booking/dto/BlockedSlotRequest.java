package com.scheduler.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlockedSlotRequest {
    @NotNull(message = "Start time is required")
    private Long startTime; // Epoch timestamp in milliseconds

    @NotNull(message = "End time is required")
    private Long endTime; // Epoch timestamp in milliseconds

    private String reason;
}
