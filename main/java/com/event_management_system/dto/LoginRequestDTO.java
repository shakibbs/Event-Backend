package com.event_management_system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * LoginRequestDTO: What the client sends during login
 * 
 * FLOW (from Diagram 3):
 * 1. Client sends email & password to POST /api/auth/login
 * 2. Spring validates this DTO (@NotBlank, @Email)
 * 3. AuthController receives this object
 * 4. AuthService uses email & password to authenticate
 * 
 * Example JSON from client:
 * {
 *     "email": "user@example.com",
 *     "password": "myPassword123"
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDTO {

    /**
     * User's email address (unique identifier for login)
     * 
     * Validation:
     * - @NotBlank: Cannot be null, empty, or just whitespace
     * - @Email: Must be a valid email format
     * 
     * If validation fails:
     * - Returns HTTP 400 with error message
     * - Example: "email should be a valid email address"
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be a valid email address")
    private String email;

    /**
     * User's password (will be compared with BCrypt hash in database)
     * 
     * Validation:
     * - @NotBlank: Cannot be null, empty, or just whitespace
     * 
     * Security notes:
     * - Never log this in production
     * - Will be compared with BCrypt hash, not stored as plain text
     * - If password doesn't match database hash → Authentication fails (HTTP 401)
     */
    @NotBlank(message = "Password is required")
    private String password;
}
