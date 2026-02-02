package com.event_management_system.mapper;

import org.springframework.stereotype.Component;

import com.event_management_system.dto.UserPasswordHistoryResponseDTO;
import com.event_management_system.entity.UserPasswordHistory;

@Component
public class UserPasswordHistoryMapper {
    
    public UserPasswordHistoryResponseDTO toDto(UserPasswordHistory entity) {
        if (entity == null) {
            return null;
        }
        
        UserPasswordHistoryResponseDTO dto = new UserPasswordHistoryResponseDTO();
        dto.setId(entity.getId());
        
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserFullName(entity.getUser().getFullName());
            dto.setUserEmail(entity.getUser().getEmail());
        }
        
        if (entity.getPasswordChangedBy() != null) {
            dto.setPasswordChangedById(entity.getPasswordChangedBy().getId());
            dto.setPasswordChangedByName(entity.getPasswordChangedBy().getFullName());
        }
        
        dto.setChangeDate(entity.getChangeDate());
        dto.setCreatedDate(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getFullName() : null);
        dto.setUpdatedDate(entity.getUpdatedAt());
        dto.setUpdatedBy(null);  // Not tracked in this entity
        
        
        return dto;
    }
}
