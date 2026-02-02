package com.event_management_system.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class UserResponseDTO {

    private Long id;
    private String fullName;
    private String email;
    private RoleResponseDTO role;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}