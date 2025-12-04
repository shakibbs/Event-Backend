package com.event_management_system.mapper;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.event_management_system.dto.PermissionResponseDTO;
import com.event_management_system.dto.RoleRequestDTO;
import com.event_management_system.dto.RoleResponseDTO;
import com.event_management_system.entity.Role;

@Component
public class RoleMapper {

    public Role toEntity(RoleRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Role role = new Role();
        role.setName(dto.getName());
        
        // Note: Permissions are handled in the service layer to avoid circular dependencies
        
        return role;
    }

    public RoleResponseDTO toDto(Role entity) {
        if (entity == null) {
            return null;
        }
        
        RoleResponseDTO dto = new RoleResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        
        // Map permissions to DTOs if they exist
        if (entity.getPermissions() != null && !entity.getPermissions().isEmpty()) {
            dto.setPermissions(entity.getPermissions().stream()
                    .map(permission -> {
                        PermissionResponseDTO permissionDto = new PermissionResponseDTO();
                        permissionDto.setId(permission.getId());
                        permissionDto.setName(permission.getName());
                        permissionDto.setStatus(permission.getStatus() != null ? permission.getStatus().toString() : null);
                        permissionDto.setCreatedAt(permission.getCreatedAt());
                        permissionDto.setCreatedBy(permission.getCreatedBy());
                        permissionDto.setUpdatedAt(permission.getUpdatedAt());
                        permissionDto.setUpdatedBy(permission.getUpdatedBy());
                        permissionDto.setDeleted(permission.getDeleted());
                        return permissionDto;
                    })
                    .collect(Collectors.toSet()));
        } else {
            dto.setPermissions(null);
        }
        
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setStatus(entity.getStatus());
        dto.setDeleted(entity.getDeleted());
        
        return dto;
    }

    public void updateEntity(RoleRequestDTO dto, Role entity) {
        if (dto == null || entity == null) {
            return;
        }
        
        entity.setName(dto.getName());
        
        // Note: Permissions are handled in the service layer to avoid circular dependencies
    }
}