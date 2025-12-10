package com.event_management_system.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserActivityHistoryResponseDTO
 * 
 * Response DTO for user activity history
 * 
 * Used in API responses to show complete activity audit trail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityHistoryResponseDTO {
    
    private Long id;
    private Long userId;
    private String username;
    private String userGroup;
    private String activityTypeCode;
    private String activityTypeName;
    private String description;
    private String deviceId;
    private LocalDateTime activityDate;
    private String ip;
    private String sessionId;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime updatedDate;
    private String updatedBy;
}
