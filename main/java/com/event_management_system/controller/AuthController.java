package com.event_management_system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.event_management_system.dto.AuthResponseDTO;
import com.event_management_system.dto.ChangePasswordRequestDTO;
import com.event_management_system.dto.LoginRequestDTO;
import com.event_management_system.dto.RefreshTokenRequestDTO;
import com.event_management_system.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * AuthController: Handle authentication endpoints
 * 
 * ENDPOINTS:
 * 1. POST /api/auth/login - User login with email & password
 * 2. POST /api/auth/refresh - Refresh access token using refresh token
 * 3. POST /api/auth/logout - Logout user and invalidate tokens
 * 
 * PUBLIC ENDPOINTS:
 * - /api/auth/login
 * - /api/auth/refresh
 * 
 * PROTECTED ENDPOINTS:
 * - /api/auth/logout (requires valid access token in Authorization header)
 * 
 * RESPONSE CODES:
 * - 200 OK: Operation successful
 * - 201 Created: Resource created (not used for auth)
 * - 400 Bad Request: Invalid input (validation failed)
 * - 401 Unauthorized: Authentication failed or token invalid
 * - 403 Forbidden: Access denied
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Login endpoint: Authenticate user with email and password
     * 
     * FLOW from Diagram 3:
     * 1. Client sends email & password
     * 2. AuthService validates credentials
     * 3. AuthService generates JWT tokens with UUID
     * 4. AuthService caches token UUIDs
     * 5. AuthService returns tokens and user info
     * 
     * REQUEST:
     * POST /api/auth/login
     * Content-Type: application/json
     * {
     *     "email": "admin@example.com",
     *     "password": "password123"
     * }
     * 
     * RESPONSE SUCCESS (HTTP 200):
     * {
     *     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *     "tokenType": "Bearer",
     *     "expiresIn": 2700,
     *     "user": {
     *         "id": 1,
     *         "email": "admin@example.com",
     *         "fullName": "Admin User",
     *         "role": "ADMIN"
     *     }
     * }
     * 
     * RESPONSE ERROR (HTTP 401):
     * {
     *     "message": "Invalid credentials"
     * }
     * 
     * VALIDATION:
     * - @Valid annotation triggers validation
     * - @NotBlank: email & password required
     * - @Email: email must be valid format
     * - If validation fails → HTTP 400
     * 
     * CLIENT USAGE:
     * 1. Receive accessToken and refreshToken
     * 2. Store in localStorage:
     *    localStorage.setItem("accessToken", accessToken);
     *    localStorage.setItem("refreshToken", refreshToken);
     * 3. Use accessToken in future requests:
     *    Authorization: Bearer <accessToken>
     * 4. When accessToken expires (45 min):
     *    - Call POST /api/auth/refresh with refreshToken
     *    - Get new accessToken
     *    - Continue with new token
     * 
     * SECURITY:
     * - Password compared using BCrypt (never plain text)
     * - Doesn't reveal if email exists ("Invalid credentials" for both cases)
     * - Tokens contain UUID for server-side logout
     * - Access token short-lived (45 min)
     * - Refresh token longer-lived (7 days)
     * 
     * @param loginRequest Email and password from client
     * @return AuthResponseDTO with tokens and user info
     * @throws RuntimeException if credentials invalid
     */
    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticate user with email and password. Returns access token, refresh token, and user details."
    )
    public ResponseEntity<AuthResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO loginRequest,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.info("Login attempt for email: {}", loginRequest.getEmail());
        
        try {
            // Call AuthService to authenticate and generate tokens
            // Pass HttpServletRequest to capture IP address and device info
            AuthResponseDTO authResponse = authService.authenticate(loginRequest, request);
            
            log.info("Login successful for email: {}", loginRequest.getEmail());
            
            // Return tokens to client
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            log.warn("Login failed for email: {} - Error: {}", loginRequest.getEmail(), e.getMessage());
            
            // Return generic error message (don't reveal if email exists)
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Refresh access token endpoint: Get new access token using refresh token
     * 
     * WHEN CALLED:
     * - Access token expires (after 45 minutes)
     * - Client doesn't want to re-login
     * - Client sends refresh token to get new access token
     * 
     * REQUEST:
     * POST /api/auth/refresh
     * Content-Type: application/json
     * {
     *     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     * 
     * RESPONSE SUCCESS (HTTP 200):
     * {
     *     "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  (NEW)
     *     "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  (SAME)
     *     "tokenType": "Bearer",
     *     "expiresIn": 2700,
     *     "user": {
     *         "id": 1,
     *         "email": "admin@example.com",
     *         "fullName": "Admin User",
     *         "role": "ADMIN"
     *     }
     * }
     * 
     * RESPONSE ERROR (HTTP 401):
     * - Refresh token is invalid
     * - Refresh token is expired (>7 days)
     * - User logged out (token UUID not in cache)
     * - User not found in database
     * 
     * FLOW:
     * 1. Client: Access token expires
     * 2. Client: POST /api/auth/refresh with refreshToken
     * 3. AuthService.refreshAccessToken(refreshToken):
     *    - Validate refresh token signature & expiration
     *    - Verify refresh token UUID in cache (not logged out)
     *    - Generate new access token
     *    - Cache new access token UUID
     *    - Return new access token (same refresh token)
     * 4. Client: Update localStorage with new access token
     * 5. Client: Continue making requests with new token
     * 
     * WHY 2 TOKENS?
     * - Access token: Short-lived (45 min)
     *   - Used for every request
     *   - If compromised, impact limited to 45 min
     * - Refresh token: Long-lived (7 days)
     *   - Used only to get new access token
     *   - If compromised, attacker has 7 days
     *   - But can't be used directly on API
     * 
     * SECURITY:
     * - Refresh token must be valid (signature & expiration)
     * - Refresh token UUID must be in cache (not logged out)
     * - New access token gets new UUID
     * - Old access token UUIDs remain valid (still in cache)
     * 
     * CLIENT USAGE:
     * 1. Store both tokens after login:
     *    localStorage.setItem("accessToken", accessToken);
     *    localStorage.setItem("refreshToken", refreshToken);
     * 
     * 2. On every request, use access token:
     *    Authorization: Bearer <accessToken>
     * 
     * 3. When server returns 401 "Token expired":
     *    a. Check if refreshToken exists
     *    b. POST /api/auth/refresh with refreshToken
     *    c. Update accessToken in localStorage
     *    d. Retry original request with new token
     * 
     * 4. If refresh fails (401):
     *    - Refresh token expired or invalid
     *    - Redirect to login page
     *    - User must login again
     * 
     * @param refreshTokenRequest Refresh token from client
     * @return AuthResponseDTO with new access token
     * @throws RuntimeException if refresh token invalid
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Get a new access token using a valid refresh token. Refresh token remains the same."
    )
    public ResponseEntity<AuthResponseDTO> refreshAccessToken(
            @Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequest) {
        
        log.info("Refresh token request received");
        
        try {
            // Call AuthService to refresh access token
            AuthResponseDTO authResponse = authService.refreshAccessToken(refreshTokenRequest.getRefreshToken());
            
            log.info("Access token refreshed successfully");
            
            return new ResponseEntity<>(authResponse, HttpStatus.OK);
            
        } catch (RuntimeException e) {
            log.warn("Token refresh failed - Error: {}", e.getMessage());
            
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Logout endpoint: Invalidate user tokens
     * 
     * WHEN CALLED:
     * - User clicks "Logout" button
     * - Client sends access token to logout endpoint
     * - Server invalidates token (removes UUID from cache)
     * 
     * REQUEST:
     * POST /api/auth/logout
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * RESPONSE SUCCESS (HTTP 200):
     * {
     *     "message": "Logged out successfully"
     * }
     * 
     * RESPONSE ERROR (HTTP 401):
     * - No Authorization header
     * - Token is invalid or expired
     * - Token already logged out
     * 
     * FLOW:
     * 1. Client: User clicks "Logout"
     * 2. Client: POST /api/auth/logout
     *    Authorization: Bearer <accessToken>
     * 3. JwtAuthenticationFilter intercepts:
     *    - Extracts token from header
     *    - Validates token (signature & expiration)
     *    - Loads user from database
     *    - Stores user in SecurityContext
     * 4. AuthController.logout():
     *    - Receives token from Authorization header
     *    - Extracts token UUID
     *    - TokenCacheService.removeTokenFromCache(uuid)
     * 5. Token is now invalidated:
     *    - UUID removed from cache
     *    - Token still cryptographically valid (but useless)
     *    - On next request, cache lookup fails
     * 6. Client: Remove tokens from localStorage
     *    localStorage.removeItem("accessToken");
     *    localStorage.removeItem("refreshToken");
     *    Redirect to login page
     * 
     * SERVER-SIDE LOGOUT:
     * - Doesn't modify the token itself
     * - Just removes UUID from cache
     * - Token becomes "logged out" instantly
     * - Works across multiple servers (if using Redis)
     * 
     * SECURITY:
     * - Requires valid access token (proves user identity)
     * - Removes UUID from cache → token useless
     * - Both access and refresh tokens invalidated
     *   (because both contain same UUID? No, different UUIDs)
     *   (but if one is removed, user is logged out)
     * - User must login again to get new tokens
     * 
     * CLIENT USAGE:
     * 1. User clicks logout button
     * 2. Client: POST /api/auth/logout
     *    Authorization: Bearer <accessToken>
     * 3. Server responds: HTTP 200
     * 4. Client removes tokens:
     *    localStorage.removeItem("accessToken");
     *    localStorage.removeItem("refreshToken");
     * 5. Redirect to login page
     * 6. If user tries to use old tokens:
     *    - Cache lookup fails (UUID deleted)
     *    - Returns 401 "Unauthorized"
     * 
     * @param authorizationHeader Authorization header with Bearer token
     * @return HTTP 200 if logout successful, 401 if token invalid
     */
    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Logout user and invalidate their tokens. Requires valid access token in Authorization header."
    )
    public ResponseEntity<?> logout(
            @Parameter(description = "Authorization header with Bearer token")
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            jakarta.servlet.http.HttpServletRequest request) {
        
        log.info("Logout request received");
        
        try {
            // Extract token from Authorization header
            String token = extractTokenFromHeader(authorizationHeader);
            
            if (token == null) {
                log.warn("Logout failed: No token in Authorization header");
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
            
            // Call AuthService to logout (removes UUID from cache)
            // Pass HttpServletRequest to capture IP and device info for logout tracking
            authService.logout(token, request);
            
            log.info("User logged out successfully");
            
            // Return success message
            return new ResponseEntity<>(
                    new LogoutResponseDTO("Logged out successfully"),
                    HttpStatus.OK
            );
            
        } catch (RuntimeException e) {
            log.warn("Logout failed - Error: {}", e.getMessage());
            
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Helper method: Extract JWT token from Authorization header
     * 
     * HEADER FORMAT:
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * EXTRACTION:
     * 1. Get Authorization header
     * 2. Check if header exists and starts with "Bearer "
     * 3. Extract token (substring after "Bearer ")
     * 4. Validate token is not empty
     * 5. Return token or null
     * 
     * @param authorizationHeader Authorization header value
     * @return JWT token string, or null if not found
     */
    private String extractTokenFromHeader(String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            if (StringUtils.hasText(token)) {
                return token;
            }
        }
        return null;
    }

    /**
     * Inner DTO: Logout response
     */
    public static class LogoutResponseDTO {
        public String message;

        public LogoutResponseDTO(String message) {
            this.message = message;
        }
    }
    
    /**
     * Change password endpoint: User changes their own password
     * 
     * FLOW:
     * 1. Extract current user ID from SecurityContext (authenticated user)
     * 2. Validate old password, new password, confirmation
     * 3. AuthService verifies old password and updates to new password
     * 4. Password change is logged in UserPasswordHistory
     * 5. Activity is logged in UserActivityHistory
     * 
     * REQUEST:
     * POST /api/auth/change-password
     * Authorization: Bearer <access-token>
     * Content-Type: application/json
     * {
     *     "oldPassword": "currentPassword123",
     *     "newPassword": "newPassword456",
     *     "confirmPassword": "newPassword456"
     * }
     * 
     * RESPONSE SUCCESS (HTTP 200):
     * {
     *     "message": "Password changed successfully"
     * }
     * 
     * RESPONSE ERROR (HTTP 400):
     * {
     *     "message": "Current password is incorrect"
     * }
     * OR
     * {
     *     "message": "New password and confirmation do not match"
     * }
     * 
     * SECURITY:
     * - Requires valid JWT token (user must be authenticated)
     * - User can only change their own password
     * - Old password must be correct
     * - New password must be different from old
     * - Password is hashed with BCrypt before storing
     * 
     * @param changePasswordRequest Request with old password, new password, confirmation
     * @return Success or error message
     */
    @PostMapping("/change-password")
    @Operation(
        summary = "Change user password",
        description = "Allows authenticated user to change their own password. Requires old password verification."
    )
    public ResponseEntity<MessageResponseDTO> changePassword(
            @Parameter(description = "Password change request with old password, new password, and confirmation")
            @Valid @RequestBody ChangePasswordRequestDTO changePasswordRequest) {
        
        try {
            // Get current authenticated user ID from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                log.error("User not authenticated");
                return new ResponseEntity<>(
                    new MessageResponseDTO("User not authenticated"),
                    HttpStatus.UNAUTHORIZED
                );
            }
            
            Long userId = Long.valueOf(authentication.getName());
            log.info("Password change request for user ID: {}", userId);
            
            // Call AuthService to change password
            authService.changePassword(
                userId,
                changePasswordRequest.getOldPassword(),
                changePasswordRequest.getNewPassword(),
                changePasswordRequest.getConfirmPassword()
            );
            
            log.info("Password changed successfully for user: {}", userId);
            
            return new ResponseEntity<>(
                new MessageResponseDTO("Password changed successfully"),
                HttpStatus.OK
            );
            
        } catch (RuntimeException e) {
            log.error("Password change failed: {}", e.getMessage());
            return new ResponseEntity<>(
                new MessageResponseDTO(e.getMessage()),
                HttpStatus.BAD_REQUEST
            );
        } catch (Exception e) {
            log.error("Unexpected error during password change: {}", e.getMessage());
            return new ResponseEntity<>(
                new MessageResponseDTO("An error occurred while changing password"),
                HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }
    
    /**
     * Inner DTO: Generic message response
     */
    public static class MessageResponseDTO {
        public String message;

        public MessageResponseDTO(String message) {
            this.message = message;
        }
    }
}

