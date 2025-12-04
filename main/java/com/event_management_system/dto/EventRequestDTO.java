package com.event_management_system.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequestDTO {

    @NotBlank(message = "Event title is required")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    @NotBlank(message = "Location is required")
    private String location;
    
    public boolean isDateRangeValid() {
        return startTime != null && endTime != null && endTime.isAfter(startTime);
    }
    
    public boolean areDatesInFuture() {
        LocalDateTime now = LocalDateTime.now();
        return startTime != null && endTime != null &&
               startTime.isAfter(now) && endTime.isAfter(now);
    }
}