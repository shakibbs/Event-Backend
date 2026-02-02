package com.event_management_system.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    private Boolean isActiveSession;
}
