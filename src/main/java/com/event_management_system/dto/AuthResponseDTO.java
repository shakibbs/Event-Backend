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

    /**
     * Access Token (JWT)
     * 
     * Purpose: Short-lived token used to access protected endpoints
     * 
     * Structure (JWT has 3 parts separated by dots):
     * - Header: {"alg":"HS256","typ":"JWT"}
     * - Payload (Claims): {"sub":"userId","tokenUuid":"uuid-123","iat":1234567890,"exp":1234567890}
     * - Signature: cryptographically signed with server secret
     * 
     * Expiration: 15 minutes (configurable in application.properties)
     * 
     * Client usage:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * When expired:
     * - Server returns HTTP 401 "Token expired"
     * - Client uses refreshToken to get new accessToken
     */
    private String accessToken;

    /**
     * Refresh Token (JWT)
     * 
     * Purpose: Long-lived token used to get a new accessToken without re-login
     * 
     * Structure: Similar to accessToken, but with longer expiration
     * 
     * Expiration: 7 days (configurable in application.properties)
     * 
     * Client usage:
     * When accessToken expires:
     * 1. POST /api/auth/refresh
     * 2. Send refreshToken in body
     * 3. Server validates refreshToken
     * 4. If valid, returns new accessToken
     * 5. Client stores new accessToken and continues
     * 
     * When both expire:
     * - User must login again with email & password
     */
    private String refreshToken;

    /**
     * Token Type: Always "Bearer"
     * 
     * HTTP Header format:
     * Authorization: Bearer <token>
     * 
     * "Bearer" is the standard token type for JWT in HTTP
     */
    private String tokenType = "Bearer";

    /**
     * Token Expiration Time (in seconds)
     * 
     * Example: 900 means 15 minutes (900 seconds)
     * 
     * Client can use this to:
     * 1. Know when token will expire
     * 2. Proactively refresh token before expiration
     * 3. Show user "Your session will expire in 5 minutes" message
     */
    private Long expiresIn;

    /**
     * User Information (without sensitive data)
     * 
     * Returned for client convenience:
     * - Client doesn't need separate call to get user info
     * - Contains: id, email, fullName, role
     * - Does NOT contain: password, database internal fields
     * 
     * Client can display user's name, email, role on dashboard
     */
    private UserResponseDTO user;
}
