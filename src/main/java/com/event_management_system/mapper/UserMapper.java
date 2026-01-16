package com.event_management_system.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.event_management_system.dto.RoleResponseDTO;
import com.event_management_system.dto.UserRequestDTO;
import com.event_management_system.dto.UserResponseDTO;
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
            roleDTO.setCreatedAt(user.getRole().getCreatedAt());
            roleDTO.setCreatedBy(user.getRole().getCreatedBy());
            roleDTO.setUpdatedAt(user.getRole().getUpdatedAt());
            roleDTO.setUpdatedBy(user.getRole().getUpdatedBy());
            userResponseDTO.setRole(roleDTO);
        }
        
        userResponseDTO.setCreatedAt(user.getCreatedAt());
        userResponseDTO.setCreatedBy(user.getCreatedBy());
        userResponseDTO.setUpdatedAt(user.getUpdatedAt());
        userResponseDTO.setUpdatedBy(user.getUpdatedBy());

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

    public UserResponseDTO toDtoWithRoles(User user) {
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
            roleDTO.setCreatedAt(user.getRole().getCreatedAt());
            roleDTO.setCreatedBy(user.getRole().getCreatedBy());
            roleDTO.setUpdatedAt(user.getRole().getUpdatedAt());
            roleDTO.setUpdatedBy(user.getRole().getUpdatedBy());
            userResponseDTO.setRole(roleDTO);
        }
        
        userResponseDTO.setCreatedAt(user.getCreatedAt());
        userResponseDTO.setCreatedBy(user.getCreatedBy());
        userResponseDTO.setUpdatedAt(user.getUpdatedAt());
        userResponseDTO.setUpdatedBy(user.getUpdatedBy());

        return userResponseDTO;
    }

    public UserResponseDTO toUserResponseDTO(User user) {
        return toDto(user);
    }
}