package com.event_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthResponseDTO: What the server sends after successful login
 * 
 * FLOW (from Diagram 3, Step 10):
 * 1. After password matches, server creates Access Token & Refresh Token
 * 2. Tokens are cached (UUID stored in Redis/Cache)
 * 3. Server returns this DTO to client
 * 4. Client stores tokens and uses them in subsequent requests
 * 
 * Example JSON response:
 * {
 *     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
 *     "tokenType": "Bearer",
 *     "expiresIn": 900,
 *     "user": {
 *         "id": 1,
 *         "email": "user@example.com",
 *         "fullName": "John Doe",
 *         "role": "ADMIN"
 *     }
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String accessToken;

    private String refreshToken;

    private String tokenType = "Bearer";

    private Long expiresIn;

    private UserResponseDTO user;
}
