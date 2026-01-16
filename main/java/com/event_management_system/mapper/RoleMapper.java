package com.event_management_system.mapper;

import java.util.Set;
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
        
        return role;
    }

    public RoleResponseDTO toDto(Role entity) {
        if (entity == null) {
            return null;
        }
        
        RoleResponseDTO dto = new RoleResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        
        // Get permissions directly from entity relationship (no service/mapper injection needed)
        if (entity.getRolePermissions() != null && !entity.getRolePermissions().isEmpty()) {
            Set<PermissionResponseDTO> permissions = entity.getRolePermissions().stream()
                    .filter(rp -> rp.getPermission() != null)
                    .map(rp -> {
                        PermissionResponseDTO permissionDto = new PermissionResponseDTO();
                        permissionDto.setId(rp.getPermission().getId());
                        permissionDto.setName(rp.getPermission().getName());
                        permissionDto.setCreatedAt(rp.getPermission().getCreatedAt());
                        permissionDto.setCreatedBy(rp.getPermission().getCreatedBy());
                        permissionDto.setUpdatedAt(rp.getPermission().getUpdatedAt());
                        permissionDto.setUpdatedBy(rp.getPermission().getUpdatedBy());
                        permissionDto.setDeleted(rp.getPermission().getDeleted());
                        return permissionDto;
                    })
                    .collect(Collectors.toSet());
            dto.setPermissions(permissions);
        } else {
            dto.setPermissions(null);
        }
        
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setDeleted(entity.getDeleted());
        
        return dto;
    }

    public void updateEntity(RoleRequestDTO dto, Role entity) {
        if (dto == null || entity == null) {
            return;
        }
        
        entity.setName(dto.getName());
    }
}