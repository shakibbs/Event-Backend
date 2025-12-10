package com.event_management_system.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserLoginLogoutHistoryResponseDTO
 * 
 * Response DTO for login/logout history
 * 
 * Used in API responses to show login sessions and security events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginLogoutHistoryResponseDTO {
    
    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String userToken;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private String requestIp;
    private String deviceInfo;
    private String loginStatus;
    
    // Helper field to check if session is still active
    private Boolean isActiveSession;
}
