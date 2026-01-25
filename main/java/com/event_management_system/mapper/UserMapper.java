package com.event_management_system.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.event_management_system.dto.RoleResponseDTO;
import com.event_management_system.dto.UserRequestDTO;
import com.event_management_system.dto.UserResponseDTO;
import com.event_management_system.dto.UserUpdateRequestDTO;
import com.event_management_system.entity.Role;
import com.event_management_system.entity.User;

@Component
public class UserMapper {
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    public User toEntity(UserRequestDTO userRequestDTO) {
        if (userRequestDTO == null) {
            return null;
        }

        User user = new User();
        user.setFullName(userRequestDTO.getFullName());
        user.setEmail(userRequestDTO.getEmail());
        
        if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        }
        
        if (userRequestDTO.getRoleId() != null) {
            Role role = new Role();
            role.setId(userRequestDTO.getRoleId());
            user.setRole(role);
        }
        
        return user;
    }

    public UserResponseDTO toDto(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDTO userResponseDTO = new UserResponseDTO();
        userResponseDTO.setId(user.getId());
        userResponseDTO.setFullName(user.getFullName());
        userResponseDTO.setEmail(user.getEmail());
        
        if (user.getRole() != null) {
            RoleResponseDTO roleDTO = new RoleResponseDTO();
            roleDTO.setId(user.getRole().getId());
            roleDTO.setName(user.getRole().getName());
            System.out.println("[UserMapper] Role ID: " + user.getRole().getId() + ", Role Name: " + user.getRole().getName());
            roleDTO.setCreatedAt(user.getRole().getCreatedAt());
            roleDTO.setCreatedBy(user.getRole().getCreatedBy());
            roleDTO.setUpdatedAt(user.getRole().getUpdatedAt());
            roleDTO.setUpdatedBy(user.getRole().getUpdatedBy());
            
            if (user.getRole().getRolePermissions() != null && !user.getRole().getRolePermissions().isEmpty()) {
                java.util.Set<com.event_management_system.dto.PermissionResponseDTO> permissions = user.getRole().getRolePermissions().stream()
                        .filter(rp -> rp.getPermission() != null)
                        .map(rp -> {
                            com.event_management_system.dto.PermissionResponseDTO permissionDto = new com.event_management_system.dto.PermissionResponseDTO();
                            permissionDto.setId(rp.getPermission().getId());
                            permissionDto.setName(rp.getPermission().getName());
                            permissionDto.setCreatedAt(rp.getPermission().getCreatedAt());
                            permissionDto.setCreatedBy(rp.getPermission().getCreatedBy());
                            permissionDto.setUpdatedAt(rp.getPermission().getUpdatedAt());
                            permissionDto.setUpdatedBy(rp.getPermission().getUpdatedBy());
                            permissionDto.setDeleted(rp.getPermission().getDeleted());
                            return permissionDto;
                        })
                        .collect(java.util.stream.Collectors.toSet());
                roleDTO.setPermissions(permissions);
                System.out.println("[UserMapper] Role " + roleDTO.getName() + " has " + permissions.size() + " permissions: " + 
                    permissions.stream().map(p -> p.getName()).collect(java.util.stream.Collectors.toList()));
            } else {
                System.out.println("[UserMapper] Role " + roleDTO.getName() + " has NO permissions assigned");
            }
            
            userResponseDTO.setRole(roleDTO);
            System.out.println("[UserMapper] RoleDTO set with name: " + roleDTO.getName());
        } else {
            System.out.println("[UserMapper] User role is null!");
        }
        
        userResponseDTO.setCreatedAt(user.getCreatedAt());
        userResponseDTO.setCreatedBy(user.getCreatedBy());
        userResponseDTO.setUpdatedAt(user.getUpdatedAt());
        userResponseDTO.setUpdatedBy(user.getUpdatedBy());

        System.out.println("[UserMapper] Returning UserResponseDTO with role: " + (userResponseDTO.getRole() != null ? userResponseDTO.getRole().getName() : "null"));
        return userResponseDTO;
    }

    public void updateEntity(UserRequestDTO userRequestDTO, User existingUser) {
        if (userRequestDTO == null || existingUser == null) {
            return;
        }

        existingUser.setFullName(userRequestDTO.getFullName());
        existingUser.setEmail(userRequestDTO.getEmail());
        
        if (userRequestDTO.getPassword() != null && !userRequestDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        }
        
        if (userRequestDTO.getRoleId() != null) {
            Role role = new Role();
            role.setId(userRequestDTO.getRoleId());
            existingUser.setRole(role);
        }
    }

    public void updateEntity(UserUpdateRequestDTO userUpdateRequestDTO, User existingUser) {
        if (userUpdateRequestDTO == null || existingUser == null) {
            return;
        }

        existingUser.setFullName(userUpdateRequestDTO.getFullName());
        existingUser.setEmail(userUpdateRequestDTO.getEmail());
    }

    public UserResponseDTO toUserResponseDTO(User user) {
        return toDto(user);
    }
}