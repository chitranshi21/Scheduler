package com.scheduler.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BusinessHoursResponse {
    private UUID id;
    private DayOfWeek dayOfWeek;
    private String startTime; // Format: "HH:mm"
    private String endTime; // Format: "HH:mm"
    private boolean enabled;
}
