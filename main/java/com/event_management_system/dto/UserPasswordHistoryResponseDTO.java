package com.event_management_system.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserPasswordHistoryResponseDTO
 * 
 * Response DTO for password change history
 * 
 * Used in API responses to show password change audit trail
 * Does NOT expose actual passwords (security)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPasswordHistoryResponseDTO {
    
    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Long passwordChangedById;
    private String passwordChangedByName;
    private LocalDateTime changeDate;
    private LocalDateTime createdDate;
    private String createdBy;
    private LocalDateTime updatedDate;
    private String updatedBy;
}
