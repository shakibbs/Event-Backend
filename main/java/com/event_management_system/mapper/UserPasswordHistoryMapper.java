package com.event_management_system.mapper;

import org.springframework.stereotype.Component;

import com.event_management_system.dto.UserPasswordHistoryResponseDTO;
import com.event_management_system.entity.UserPasswordHistory;

/**
 * UserPasswordHistoryMapper
 * 
 * Maps UserPasswordHistory entity to UserPasswordHistoryResponseDTO
 * 
 * Security: Does NOT expose actual password hashes in DTO
 */
@Component
public class UserPasswordHistoryMapper {
    
    /**
     * Convert entity to DTO for API response
     * 
     * @param entity - UserPasswordHistory entity
     * @return UserPasswordHistoryResponseDTO
     */
    public UserPasswordHistoryResponseDTO toDto(UserPasswordHistory entity) {
        if (entity == null) {
            return null;
        }
        
        UserPasswordHistoryResponseDTO dto = new UserPasswordHistoryResponseDTO();
        dto.setId(entity.getId());
        
        // User info
        if (entity.getUser() != null) {
            dto.setUserId(entity.getUser().getId());
            dto.setUserFullName(entity.getUser().getFullName());
            dto.setUserEmail(entity.getUser().getEmail());
        }
        
        // Changed by info
        if (entity.getPasswordChangedBy() != null) {
            dto.setPasswordChangedById(entity.getPasswordChangedBy().getId());
            dto.setPasswordChangedByName(entity.getPasswordChangedBy().getFullName());
        }
        
        // Timestamps
        dto.setChangeDate(entity.getChangeDate());
        dto.setCreatedDate(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy() != null ? entity.getCreatedBy().getFullName() : null);
        dto.setUpdatedDate(entity.getUpdatedAt());
        dto.setUpdatedBy(null);  // Not tracked in this entity
        
        // Security: Do NOT expose password hashes
        // oldPassword and newPassword are intentionally excluded
        
        return dto;
    }
}
