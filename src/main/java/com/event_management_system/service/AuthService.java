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


@Service
public class AuthService {

    @Autowired
    private ApplicationLoggerService log;

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
        log.trace("[AuthService] TRACE - authenticate() called with email=" + loginRequest.getEmail());
        log.debug("[AuthService] DEBUG - authenticate() - Validating input parameters");
        if (loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
            log.warn("[AuthService] WARN - Authentication attempt with null email or password");
            throw new RuntimeException("Email and password are required");
        }

        log.debug("[AuthService] DEBUG - authenticate() - Searching for user with email: " + loginRequest.getEmail());
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> {
                    log.warn("[AuthService] WARN - User not found with email: " + loginRequest.getEmail());
                    return new RuntimeException("Invalid credentials");
                });

        log.debug("[AuthService] DEBUG - authenticate() - User found: userId=" + user.getId());

        log.debug("[AuthService] DEBUG - authenticate() - Validating password for userId=" + user.getId());
        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            log.warn("[AuthService] WARN - Invalid password for user: " + user.getId());
            throw new RuntimeException("Invalid credentials");
        }

        log.info("[AuthService] INFO - Password validated successfully for userId=" + user.getId());

        log.debug("[AuthService] DEBUG - authenticate() - Generating access token");
        String roleName = user.getRole() != null ? user.getRole().getName() : "USER";
        Long roleId = user.getRole() != null ? user.getRole().getId() : null;
        String accessToken = jwtService.generateAccessToken(user.getId(), roleName, roleId);
        log.debug("[AuthService] DEBUG - authenticate() - Access token generated for userId=" + user.getId() + " with role=" + roleName);

        log.debug("[AuthService] DEBUG - authenticate() - Generating refresh token");
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        log.debug("[AuthService] DEBUG - authenticate() - Refresh token generated for userId=" + user.getId());

        String accessTokenUuid = jwtService.getTokenUuidFromToken(accessToken);
        String refreshTokenUuid = jwtService.getTokenUuidFromToken(refreshToken);

        log.debug("[AuthService] DEBUG - authenticate() - Token UUIDs extracted - Access UUID=" + accessTokenUuid + ", Refresh UUID=" + refreshTokenUuid);

        tokenCacheService.cacheAccessToken(accessTokenUuid, user.getId());
        tokenCacheService.cacheRefreshToken(refreshTokenUuid, user.getId());

        log.info("[AuthService] INFO - Tokens cached successfully for userId=" + user.getId());
        
        if (request != null) {
            try {
                String ipAddress = requestInfoUtil.getClientIpAddress(request);
                String deviceInfo = requestInfoUtil.getCompleteDeviceInfo(request);
                String deviceId = requestInfoUtil.generateDeviceId(request);
                
                log.debug("[AuthService] DEBUG - authenticate() - Recording login history for userId=" + user.getId());
                
                loginLogoutHistoryService.recordLogin(
                    user,
                    Objects.requireNonNull(accessTokenUuid, "Access token UUID should not be null"),
                    Objects.requireNonNull(ipAddress, "IP address should not be null"),
                    Objects.requireNonNull(deviceInfo, "Device info should not be null"),
                    "SUCCESS"
                );
                
                activityHistoryService.recordActivity(
                    user,
                    com.event_management_system.entity.UserActivityHistory.ActivityType.USER_LOGIN,
                    "Logged in from " + deviceInfo,
                    ipAddress,
                    deviceId,
                    accessTokenUuid
                );
                
                log.debug("[AuthService] DEBUG - authenticate() - Login history recorded for userId=" + user.getId());
            } catch (Exception e) {
                log.error("[AuthService] ERROR - Failed to record login history: " + e.getMessage(), e);
            }
        }

        var userResponseDTO = userMapper.toUserResponseDTO(user);

        AuthResponseDTO authResponseDTO = new AuthResponseDTO();
        authResponseDTO.setAccessToken(accessToken);
        authResponseDTO.setRefreshToken(refreshToken);
        authResponseDTO.setTokenType("Bearer");
        authResponseDTO.setExpiresIn(45 * 60L);
        authResponseDTO.setUser(userResponseDTO);

        log.info("[AuthService] INFO - Authentication successful for userId=" + user.getId() + ", email=" + user.getEmail());

        return authResponseDTO;
    }

    public AuthResponseDTO authenticate(LoginRequestDTO loginRequest) {
        return authenticate(loginRequest, null);
    }

  
    public AuthResponseDTO refreshAccessToken(String refreshToken) {
        log.info("Attempting to refresh access token");

        if (!jwtService.validateToken(refreshToken)) {
            log.warn("Refresh token is invalid or expired");
            throw new RuntimeException("Refresh token is invalid or expired");
        }

        log.debug("Refresh token validated successfully");

        Long userId = jwtService.getUserIdFromToken(refreshToken);
        String refreshTokenUuid = jwtService.getTokenUuidFromToken(refreshToken);

        log.debug("Extracted user ID: {} and token UUID: {} from refresh token", userId, refreshTokenUuid);

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

        String newAccessToken = jwtService.generateAccessToken(userId);
        log.debug("New access token generated for user: {}", userId);

        String newAccessTokenUuid = jwtService.getTokenUuidFromToken(newAccessToken);
        tokenCacheService.cacheAccessToken(newAccessTokenUuid, userId);

        log.info("New access token cached for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var userResponseDTO = userMapper.toUserResponseDTO(user);

        AuthResponseDTO authResponseDTO = new AuthResponseDTO();
        authResponseDTO.setAccessToken(newAccessToken);
        authResponseDTO.setRefreshToken(refreshToken);
        authResponseDTO.setTokenType("Bearer");
        authResponseDTO.setExpiresIn(45 * 60L);
        authResponseDTO.setUser(userResponseDTO);

        log.info("Access token refreshed successfully for user: {}", userId);

        return authResponseDTO;
    }

   
    public void logout(String token, jakarta.servlet.http.HttpServletRequest request) {
        log.info("Attempting logout");

        if (!jwtService.validateToken(token)) {
            log.warn("Token is invalid or expired, cannot logout");
            throw new RuntimeException("Invalid token");
        }

        String tokenUuid = jwtService.getTokenUuidFromToken(token);
        Long userId = jwtService.getUserIdFromToken(token);
        log.debug("Extracted token UUID: {}, User ID: {}", tokenUuid, userId);

        tokenCacheService.removeTokenFromCache(tokenUuid);
        
        loginLogoutHistoryService.recordLogout(
            Objects.requireNonNull(tokenUuid, "Token UUID should not be null"));
        
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
                        tokenUuid
                    );
                    
                    log.info("Logout activity recorded for user: {}", userId);
                }
            } catch (Exception e) {
                log.error("Failed to record logout activity: {}", e.getMessage(), e);
            }
        }

        log.info("User logged out successfully, token UUID removed from cache");
    }
    
    public void logout(String token) {
        logout(token, null);
    }

    public boolean userExistsByEmail(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

   
    public int getActiveTokenCount() {
        return tokenCacheService.getTokenCacheSize();
    }
    
  
    public void changePassword(Long userId, String oldPassword, String newPassword, String confirmPassword) {
        log.info("Processing password change for user ID: {}", userId);
        
        if (userId == null) {
            throw new RuntimeException("User ID cannot be null");
        }
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("Password change failed for user {}: Incorrect old password", userId);
            throw new RuntimeException("Current password is incorrect");
        }
        
        if (!newPassword.equals(confirmPassword)) {
            log.warn("Password change failed for user {}: Passwords do not match", userId);
            throw new RuntimeException("New password and confirmation do not match");
        }
        
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            log.warn("Password change failed for user {}: New password same as old", userId);
            throw new RuntimeException("New password must be different from current password");
        }
        
        String oldPasswordHash = user.getPassword();
        
        String newPasswordHash = passwordEncoder.encode(newPassword);
        
        user.setPassword(newPasswordHash);
        user.recordUpdate(user.getFullName());
        userRepository.save(user);
        
        log.info("Password updated successfully for user: {}", userId);
        
        try {
            if (newPasswordHash != null) {
                passwordHistoryService.recordPasswordChange(
                    user,
                    user,
                    oldPasswordHash,
                    newPasswordHash
                );
                log.info("Password change recorded in history for user: {}", userId);
            }
        } catch (Exception e) {
            log.error("Failed to record password change history for user {}: {}", userId, e.getMessage(), e);
        }
        
        try {
            activityHistoryService.recordActivity(
                user,
                com.event_management_system.entity.UserActivityHistory.ActivityType.PASSWORD_CHANGED,
                "Password changed successfully",
                "0.0.0.0",
                "system",
                ""
            );
            log.info("Password change activity recorded for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to record password change activity for user {}: {}", userId, e.getMessage(), e);
        }
    }
}



