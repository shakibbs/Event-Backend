package com.event_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * ChangePasswordRequestDTO
 * 
 * Request body for user password change
 * 
 * Fields:
 * - oldPassword: Current password (for verification)
 * - newPassword: New password to set
 * - confirmPassword: Confirmation of new password
 * 
 * Validation:
 * - All fields required
 * - New password minimum 6 characters
 */
@Data
public class ChangePasswordRequestDTO {
    
    @NotBlank(message = "Current password is required")
    private String oldPassword;
    
    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;
    
    @NotBlank(message = "Password confirmation is required")
    private String confirmPassword;
}
