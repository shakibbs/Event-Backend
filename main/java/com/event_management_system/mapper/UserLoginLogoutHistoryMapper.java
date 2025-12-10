package com.event_management_system.mapper;

import org.springframework.stereotype.Component;

import com.event_management_system.dto.UserLoginLogoutHistoryResponseDTO;
import com.event_management_system.entity.UserLoginLogoutHistory;

/**
 * UserLoginLogoutHistoryMapper
 * 
 * Maps UserLoginLogoutHistory entity to UserLoginLogoutHistoryResponseDTO
 */
@Component
public class UserLoginLogoutHistoryMapper {
    
    /**
     * Convert entity to DTO for API response
     * 
     * @param entity - UserLoginLogoutHistory entity
     * @return UserLoginLogoutHistoryResponseDTO
     */
    public UserLoginLogoutHistoryResponseDTO toDto(UserLoginLogoutHistory entity) {
        if (entity == null) {
            return null;
        }
        
        UserLoginLogoutHistoryResponseDTO dto = new UserLoginLogoutHistoryResponseDTO();
        dto.setId(entity.getId());
        
        // User info
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserFullName(entity.getUser().getFullName());
            dto.setUserEmail(entity.getUser().getEmail());
        }
        
        // Session info
        dto.setUserToken(entity.getUserToken());
        dto.setLoginTime(entity.getLoginTime());
        dto.setLogoutTime(entity.getLogoutTime());
        dto.setRequestIp(entity.getRequestIp());
        dto.setDeviceInfo(entity.getDeviceInfo());
        dto.setLoginStatus(entity.getLoginStatus());
        
        // Calculate if session is active (logoutTime is null)
        dto.setIsActiveSession(entity.getLogoutTime() == null);
        
        return dto;
    }
}
