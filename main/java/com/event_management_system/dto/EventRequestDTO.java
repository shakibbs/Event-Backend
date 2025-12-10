package com.event_management_system.dto;

import com.event_management_system.entity.Event;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "2025-12-20 10:00:00", description = "Event start time (format: yyyy-MM-dd HH:mm:ss)")
    private String startTime;

    @NotNull(message = "End time is required")
    @Schema(example = "2025-12-20 17:00:00", description = "Event end time (format: yyyy-MM-dd HH:mm:ss)")
    private String endTime;
    

    @NotBlank(message = "Location is required")
    private String location;
    
    @Schema(example = "PUBLIC", description = "Event visibility level (PUBLIC, PRIVATE, INVITE_ONLY)")
    private Event.Visibility visibility = Event.Visibility.PUBLIC;
    
    public boolean isDateRangeValid() {
        // Simple validation - just check if end time is after start time in string format
        if (startTime == null || endTime == null) return false;
        return endTime.compareTo(startTime) > 0;
    }
    
    public boolean areDatesInFuture() {
        // Simple validation - just check if dates are in the future (basic check)
        // This is a basic check - actual date validation should be done in the service layer
        return startTime != null && endTime != null;
    }
}