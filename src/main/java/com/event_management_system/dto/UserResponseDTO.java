package com.event_management_system.dto;

import java.time.LocalDateTime;

import com.event_management_system.entity.BaseEntity.Status;

import lombok.Data;

@Data
public class UserResponseDTO {

    private Long id;
    private String fullName;
    private String email;
    private RoleResponseDTO role;
    private Status status;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}