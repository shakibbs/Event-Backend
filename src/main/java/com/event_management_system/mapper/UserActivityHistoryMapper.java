package com.event_management_system.mapper;

import org.springframework.stereotype.Component;

import com.event_management_system.dto.UserActivityHistoryResponseDTO;
import com.event_management_system.entity.UserActivityHistory;

@Component
public class UserActivityHistoryMapper {
    
    public UserActivityHistoryResponseDTO toDto(UserActivityHistory entity) {
        if (entity == null) {
            return null;
        }
        
        UserActivityHistoryResponseDTO dto = new UserActivityHistoryResponseDTO();
        dto.setId(entity.getId());
        
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
        }
        
        dto.setUsername(entity.getUsername());
        dto.setUserGroup(entity.getUserGroup());
        
        dto.setActivityTypeCode(entity.getActivityTypeCode());
        dto.setActivityTypeName(entity.getActivityTypeName());
        dto.setDescription(entity.getDescription());
        dto.setDeviceId(entity.getDeviceId());
        dto.setActivityDate(entity.getActivityDate());
        dto.setIp(entity.getIp());
        dto.setSessionId(entity.getSessionId());
        
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setCreatedBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getFullName() : null);
        dto.setUpdatedDate(entity.getUpdatedDate());
        dto.setUpdatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy().getFullName() : null);
        
        return dto;
    }
}
