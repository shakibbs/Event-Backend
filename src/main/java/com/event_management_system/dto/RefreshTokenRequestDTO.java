package com.event_management_system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RefreshTokenRequestDTO: Request to refresh access token
 * 
 * Used when access token expires and client needs a new one
 * 
 * Example JSON from client:
 * {
 *     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequestDTO {

    /**
     * Refresh token from previous login
     * 
     * Validation:
     * - @NotBlank: Cannot be null or empty
     * 
     * This token should:
     * - Be valid (not tampered with)
     * - Not be expired (7 days from issue)
     * - Have UUID in cache (not logged out)
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
