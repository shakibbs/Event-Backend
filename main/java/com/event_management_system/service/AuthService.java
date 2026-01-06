package com.event_management_system.service;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.event_management_system.dto.AuthResponseDTO;
import com.event_management_system.dto.LoginRequestDTO;
import com.event_management_system.entity.User;
import com.event_management_system.mapper.UserMapper;
import com.event_management_system.repository.UserRepository;
import com.event_management_system.util.RequestInfoUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * AuthService: Handle authentication logic (login, refresh, logout)
 * 
 * IMPLEMENTS DIAGRAM 3: Login Flow
 * 
 * PURPOSE:
 * Authenticate user with email & password
 * Generate JWT tokens (access + refresh)
 * Cache token UUIDs for server-side logout
 * Return tokens and user info to client
 * 
 * FLOW from Diagram 3:
 * 
 * STEP 1: Email & Password Validation
 * - Validate email and password are provided (not empty)
 * - If validation fails → return error
 * 
 * STEP 2: Fetch User by Email
 * - Query database: SELECT * FROM app_users WHERE email = ?
 * - If not found → throw UsernameNotFoundException
 * 
 * STEP 3: Found? Check
 * - If user exists → continue
 * - If not found → return error (HTTP 401)
 * 
 * STEP 4: Compare Password
 * - Compare incoming password with BCrypt hash in database
 * - PasswordEncoder.matches(incomingPassword, storedHash)
 * - If matches → continue
 * - If not matches → return error (HTTP 401)
 * 
 * STEP 5: Matched? Check
 * - If password correct → continue
 * - If incorrect → return error (HTTP 401)
 * 
 * STEP 6: Create Token Access and Refresh Token
 * - JwtService.generateAccessToken(userId)
 * - JwtService.generateRefreshToken(userId)
 * - Both tokens contain UUID for logout
 * 
 * STEP 7: Cache Tokens UUID
 * - TokenCacheService.cacheAccessToken(uuid, userId, 45min)
 * - TokenCacheService.cacheRefreshToken(uuid, userId, 7days)
 * 
 * STEP 8: Cache User
 * - Optional: Can cache user details for faster access
 * - For now, we fetch from database on each request
 * 
 * STEP 9: Return to User
 * - Return AuthResponseDTO with:
 *   - accessToken
 *   - refreshToken
 *   - tokenType: "Bearer"
 *   - expiresIn: 2700 (seconds, 45 minutes)
 *   - user: UserResponseDTO
 * 
 * SECURITY NOTES:
 * 
 * Password Encoding:
 * - Never compare plain text passwords
 * - Always use BCrypt or similar
 * - BCrypt automatically salts the password
 * - Same password produces different hashes (due to salt)
 * - PasswordEncoder.matches() compares safely
 * 
 * Error Messages:
 * - Always say "Invalid credentials" (don't reveal if email exists)
 * - Prevents user enumeration attacks
 * - Attacker can't determine valid emails
 * 
 * Token Generation:
 * - Each token gets a unique UUID
 * - Enables server-side logout
 * - Tokens don't expire in app logic, only by timestamp
 * 
 * Caching:
 * - Both tokens cached with their UUID as key
 * - On every request, token UUID is looked up in cache
 * - If not in cache → user logged out or token invalid
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private ApplicationLoggerService logger;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private TokenCacheService tokenCacheService;

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private UserLoginLogoutHistoryService loginLogoutHistoryService;
    
    @Autowired
    private UserActivityHistoryService activityHistoryService;
    
    @Autowired
    private RequestInfoUtil requestInfoUtil;
    
    @Autowired
    private UserPasswordHistoryService passwordHistoryService;

 
    public AuthResponseDTO authenticate(LoginRequestDTO loginRequest, jakarta.servlet.http.HttpServletRequest request) {
        // TRACE: Entry point
        logger.trace("[AuthService] TRACE - authenticate() called with email=" + loginRequest.getEmail());

        // DEBUG: Validate input
        logger.debug("[AuthService] DEBUG - authenticate() - Validating input parameters");
        if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            logger.warn("[AuthService] WARN - Authentication attempt with null email or password");
            throw new RuntimeException("Email and password are required");
        }

        // DEBUG: Search for user by email
        logger.debug("[AuthService] DEBUG - authenticate() - Searching for user with email: " + loginRequest.getEmail());
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    logger.warn("[AuthService] WARN - User not found with email: " + loginRequest.getEmail());
                    // Don't reveal if email exists (security)
                    return new RuntimeException("Invalid credentials");
                });

        logger.debug("[AuthService] DEBUG - authenticate() - User found: userId=" + user.getId());

        // DEBUG: Validate password
        logger.debug("[AuthService] DEBUG - authenticate() - Validating password for userId=" + user.getId());
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            logger.warn("[AuthService] WARN - Invalid password for user: " + user.getId());
            // Don't reveal if password is wrong (security)
            throw new RuntimeException("Invalid credentials");
        }

        logger.info("[AuthService] INFO - Password validated successfully for userId=" + user.getId());

        // DEBUG: Generate access token
        logger.debug("[AuthService] DEBUG - authenticate() - Generating access token");
        String accessToken = jwtService.generateAccessToken(user.getId());
        logger.debug("[AuthService] DEBUG - authenticate() - Access token generated for userId=" + user.getId());

        // DEBUG: Generate refresh token
        logger.debug("[AuthService] DEBUG - authenticate() - Generating refresh token");
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        logger.debug("[AuthService] DEBUG - authenticate() - Refresh token generated for userId=" + user.getId());

        // DEBUG: Extract and cache token UUIDs
        String accessTokenUuid = jwtService.getTokenUuidFromToken(accessToken);
        String refreshTokenUuid = jwtService.getTokenUuidFromToken(refreshToken);

        logger.debug("[AuthService] DEBUG - authenticate() - Token UUIDs extracted - Access UUID=" + accessTokenUuid + ", Refresh UUID=" + refreshTokenUuid);

        // Cache tokens with their UUIDs
        tokenCacheService.cacheAccessToken(accessTokenUuid, user.getId());
        tokenCacheService.cacheRefreshToken(refreshTokenUuid, user.getId());

        logger.info("[AuthService] INFO - Tokens cached successfully for userId=" + user.getId());
        
        // Record login history and activity
        if (request != null) {
            try {
                // Extract IP and device info
                String ipAddress = requestInfoUtil.getClientIpAddress(request);
                String deviceInfo = requestInfoUtil.getCompleteDeviceInfo(request);
                String deviceId = requestInfoUtil.generateDeviceId(request);
                
                logger.debug("[AuthService] DEBUG - authenticate() - Recording login history for userId=" + user.getId());
                
                // Record login in UserLoginLogoutHistory
                loginLogoutHistoryService.recordLogin(
                    user,
                    Objects.requireNonNull(accessTokenUuid, "Access token UUID should not be null"),
                    Objects.requireNonNull(ipAddress, "IP address should not be null"),
                    Objects.requireNonNull(deviceInfo, "Device info should not be null"),
                    "SUCCESS"
                );
                
                // Record activity in UserActivityHistory
                activityHistoryService.recordActivity(
                    user,
                    com.event_management_system.entity.UserActivityHistory.ActivityType.USER_LOGIN,
                    "Logged in from " + deviceInfo,
                    ipAddress,
                    deviceId,
                    accessTokenUuid  // Session ID
                );
                
                logger.debug("[AuthService] DEBUG - authenticate() - Login history recorded for userId=" + user.getId());
            } catch (Exception e) {
                // Don't fail login if history recording fails
                logger.error("[AuthService] ERROR - Failed to record login history: " + e.getMessage());
            }
        }

        // Build response DTO
        var userResponseDTO = userMapper.toUserResponseDTO(user);

        AuthResponseDTO authResponseDTO = new AuthResponseDTO();
        authResponseDTO.setAccessToken(accessToken);
        authResponseDTO.setRefreshToken(refreshToken);
        authResponseDTO.setTokenType("Bearer");
        authResponseDTO.setExpiresIn(45 * 60L);  // 45 minutes in seconds
        authResponseDTO.setUser(userResponseDTO);

        logger.info("[AuthService] INFO - Authentication successful for userId=" + user.getId() + ", email=" + user.getEmail());

        return authResponseDTO;
    }

    /**
     * Authenticate user with email and password (backward compatibility - no request context)
     * 
     * This method exists for backward compatibility or when request context is not available.
     * It calls the main authenticate method with null request.
     * 
     * @param loginRequest Contains email and password from client
     * @return AuthResponseDTO with tokens and user info
     * @throws RuntimeException if credentials invalid
     */
    public AuthResponseDTO authenticate(LoginRequestDTO loginRequest) {
        return authenticate(loginRequest, null);
    }

    /**
     * Refresh access token using refresh token
     * 
     * WHEN CALLED:
     * - Access token expires (45 minutes)
     * - Client sends refresh token to POST /api/auth/refresh
     * - AuthController calls authService.refreshAccessToken(refreshToken)
     * 
     * PROCESS:
     * 1. Validate refresh token (signature & expiration)
     * 2. Extract user ID from refresh token
     * 3. Generate new access token
     * 4. Cache new access token UUID
     * 5. Return new access token
     * 
     * WHY refresh token?
     * - Access token is short-lived (45 min)
     * - User doesn't have to re-login when it expires
     * - Use refresh token to get new access token
     * - If refresh token compromised, user must login again (7 days later)
     * 
     * @param refreshToken Refresh token from client
     * @return AuthResponseDTO with new access token
     * @throws RuntimeException if refresh token invalid/expired
     */
    public AuthResponseDTO refreshAccessToken(String refreshToken) {
        log.info("Attempting to refresh access token");

        // STEP 1: Validate refresh token
        if (!jwtService.validateToken(refreshToken)) {
            log.warn("Refresh token is invalid or expired");
            throw new RuntimeException("Refresh token is invalid or expired");
        }

        log.debug("Refresh token validated successfully");

        // STEP 2: Extract user ID and token UUID from refresh token
        Long userId = jwtService.getUserIdFromToken(refreshToken);
        String refreshTokenUuid = jwtService.getTokenUuidFromToken(refreshToken);

        log.debug("Extracted user ID: {} and token UUID: {} from refresh token", userId, refreshTokenUuid);

        // STEP 3: Verify refresh token is in cache (not logged out)
        Long cachedUserId = tokenCacheService.getUserIdFromCache(refreshTokenUuid);
        if (cachedUserId == null) {
            log.warn("Refresh token not found in cache (user may have logged out)");
            throw new RuntimeException("Refresh token is invalid or expired");
        }

        if (!userId.equals(cachedUserId)) {
            log.error("User ID mismatch in refresh token");
            throw new RuntimeException("Invalid refresh token");
        }

        log.debug("Refresh token verified in cache");

        // STEP 4: Generate new access token
        String newAccessToken = jwtService.generateAccessToken(userId);
        log.debug("New access token generated for user: {}", userId);

        // STEP 5: Extract UUID and cache it
        String newAccessTokenUuid = jwtService.getTokenUuidFromToken(newAccessToken);
        tokenCacheService.cacheAccessToken(newAccessTokenUuid, userId);

        log.info("New access token cached for user: {}", userId);

        // STEP 6: Build response
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var userResponseDTO = userMapper.toUserResponseDTO(user);

        AuthResponseDTO authResponseDTO = new AuthResponseDTO();
        authResponseDTO.setAccessToken(newAccessToken);
        authResponseDTO.setRefreshToken(refreshToken);  // Send back same refresh token
        authResponseDTO.setTokenType("Bearer");
        authResponseDTO.setExpiresIn(45 * 60L);  // 45 minutes in seconds
        authResponseDTO.setUser(userResponseDTO);

        log.info("Access token refreshed successfully for user: {}", userId);

        return authResponseDTO;
    }

    /**
     * Logout user by invalidating tokens (with request context for activity tracking)
     * 
     * WHEN CALLED:
     * - User clicks "Logout" button
     * - Client sends token to POST /api/auth/logout
     * - AuthController calls authService.logout(token, request)
     * 
     * PROCESS:
     * 1. Validate token (signature & expiration)
     * 2. Extract token UUID from token
     * 3. Delete UUID from cache
     * 4. Record logout in history
     * 5. Record activity
     * 
     * @param token Access token to logout
     * @param request HttpServletRequest for extracting IP and device info
     * @throws RuntimeException if token invalid
     */
    public void logout(String token, jakarta.servlet.http.HttpServletRequest request) {
        log.info("Attempting logout");

        // STEP 1: Validate token
        if (!jwtService.validateToken(token)) {
            log.warn("Token is invalid or expired, cannot logout");
            throw new RuntimeException("Invalid token");
        }

        // STEP 2: Extract token UUID and user ID
        String tokenUuid = jwtService.getTokenUuidFromToken(token);
        Long userId = jwtService.getUserIdFromToken(token);
        log.debug("Extracted token UUID: {}, User ID: {}", tokenUuid, userId);

        // STEP 3: Remove from cache (invalidate token)
        tokenCacheService.removeTokenFromCache(tokenUuid);
        
        // STEP 4: Record logout in history
        // Update logout time for this session
        loginLogoutHistoryService.recordLogout(
            Objects.requireNonNull(tokenUuid, "Token UUID should not be null"));
        
        // STEP 5: Record logout activity
        if (request != null && userId != null) {
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null) {
                    String ipAddress = requestInfoUtil.getClientIpAddress(request);
                    String deviceId = requestInfoUtil.generateDeviceId(request);
                    
                    activityHistoryService.recordActivity(
                        user,
                        com.event_management_system.entity.UserActivityHistory.ActivityType.USER_LOGOUT,
                        "Logged out",
                        ipAddress,
                        deviceId,
                        tokenUuid  // Session ID
                    );
                    
                    log.info("Logout activity recorded for user: {}", userId);
                }
            } catch (Exception e) {
                // Don't fail logout if activity recording fails
                log.error("Failed to record logout activity: {}", e.getMessage());
            }
        }

        log.info("User logged out successfully, token UUID removed from cache");
    }
    
    /**
     * Logout user by invalidating tokens (backward compatibility - no request context)
     * 
     * @param token Access token to logout
     * @throws RuntimeException if token invalid
     */
    public void logout(String token) {
        logout(token, null);
    }

    /**
     * Helper method: Check if user exists by email
     * 
     * WHEN CALLED:
     * - During registration validation
     * - To prevent duplicate emails
     * 
     * @param email Email to check
     * @return true if user exists, false otherwise
     */
    public boolean userExistsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * Helper method: Get active token count (for monitoring)
     * 
     * WHEN CALLED:
     * - Admin dashboard
     * - Monitoring system
     * - Debugging
     * 
     * @return Number of active tokens in cache
     */
    public int getActiveTokenCount() {
        return tokenCacheService.getTokenCacheSize();
    }
    
    /**
     * Change user password
     * 
     * WHEN CALLED:
     * - User wants to change their password
     * - Called from POST /api/auth/change-password
     * 
     * FLOW:
     * 1. Verify old password is correct
     * 2. Verify new password and confirmation match
     * 3. Hash new password with BCrypt
     * 4. Update user password in database
     * 5. Record password change in history
     * 6. Record activity in user activity history
     * 
     * SECURITY:
     * - Old password must match current password
     * - New password must be different from old
     * - Password is hashed with BCrypt before storing
     * - Password change is logged for audit trail
     * 
     * @param userId - ID of user changing password
     * @param oldPassword - Current password (for verification)
     * @param newPassword - New password to set
     * @param confirmPassword - Confirmation of new password
     * @throws RuntimeException if validation fails
     */
    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        log.info("Processing password change for user ID: {}", userId);
        
        // Validate userId is not null
        if (userId == null) {
            throw new RuntimeException("User ID cannot be null");
        }
        
        // Step 1: Fetch user
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Step 2: Verify old password is correct
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Password change failed for user {}: Incorrect old password", userId);
            throw new RuntimeException("Current password is incorrect");
        }
        
        // Step 3: Verify new password and confirmation match
        if (!newPassword.equals(confirmPassword)) {
            log.warn("Password change failed for user {}: Passwords do not match", userId);
            throw new RuntimeException("New password and confirmation do not match");
        }
        
        // Step 4: Verify new password is different from old
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("Password change failed for user {}: New password same as old", userId);
            throw new RuntimeException("New password must be different from current password");
        }
        
        // Step 5: Store old password hash for history
        String oldPasswordHash = user.getPassword();
        
        // Step 6: Hash new password with BCrypt
        String newPasswordHash = passwordEncoder.encode(newPassword);
        
        // Step 7: Update user password
        user.setPassword(newPasswordHash);
        user.recordUpdate(user.getFullName());
        userRepository.save(user);
        
        log.info("Password updated successfully for user: {}", userId);
        
        // Step 8: Record password change in history
        try {
            if (newPasswordHash != null) {
                passwordHistoryService.recordPasswordChange(
                    user,               // User whose password changed
                    user,               // Changed by themselves
                    oldPasswordHash,    // Old password hash
                    newPasswordHash     // New password hash
                );
                log.info("Password change recorded in history for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to record password change history for user {}: {}", userId, e.getMessage());
            // Don't fail the password change if history recording fails
        }
        
        // Step 9: Record activity in user activity history
        try {
            activityHistoryService.recordActivity(
                user,
                com.event_management_system.entity.UserActivityHistory.ActivityType.PASSWORD_CHANGED,
                "Password changed successfully",
                "0.0.0.0",  // IP not available in this context
                "system",   // Device ID not available
                ""          // Session ID not available (empty string instead of null)
            );
            log.info("Password change activity recorded for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to record password change activity for user {}: {}", userId, e.getMessage());
            // Don't fail the password change if activity recording fails
        }
    }
}
