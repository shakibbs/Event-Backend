package com.event_management_system.mapper;

import org.springframework.stereotype.Component;

import com.event_management_system.dto.UserLoginLogoutHistoryResponseDTO;
import com.event_management_system.entity.UserLoginLogoutHistory;

@Component
public class UserLoginLogoutHistoryMapper {
    
    public UserLoginLogoutHistoryResponseDTO toDto(UserLoginLogoutHistory entity) {
        if (entity == null) {
            return null;
        }
        
        UserLoginLogoutHistoryResponseDTO dto = new UserLoginLogoutHistoryResponseDTO();
        dto.setId(entity.getId());
        
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserFullName(entity.getUser().getFullName());
            dto.setUserEmail(entity.getUser().getEmail());
        }
        
        dto.setUserToken(entity.getUserToken());
        dto.setLoginTime(entity.getLoginTime());
        dto.setLogoutTime(entity.getLogoutTime());
        dto.setRequestIp(entity.getRequestIp());
        dto.setDeviceInfo(entity.getDeviceInfo());
        dto.setLoginStatus(entity.getLoginStatus());
        
        dto.setIsActiveSession(entity.getLogoutTime() == null);
        
        return dto;
    }
}

