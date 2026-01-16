package com.event_management_system.mapper;

import org.springframework.stereotype.Component;

import com.event_management_system.dto.PermissionRequestDTO;
import com.event_management_system.dto.PermissionResponseDTO;
import com.event_management_system.entity.Permission;

@Component
public class PermissionMapper {
    
    public Permission toEntity(PermissionRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Permission permission = new Permission();
        permission.setName(dto.getName());
        
        return permission;
    }
    
    public PermissionResponseDTO toDto(Permission entity) {
        if (entity == null) {
            return null;
        }
        
        PermissionResponseDTO dto = new PermissionResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setDeleted(entity.getDeleted());
        
        return dto;
    }
    
    public void updateEntity(PermissionRequestDTO dto, Permission entity) {
        if (dto == null || entity == null) {
            return;
        }
        
        entity.setName(dto.getName());
    }
}