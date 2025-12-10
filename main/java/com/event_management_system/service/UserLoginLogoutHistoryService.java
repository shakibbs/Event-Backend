package com.event_management_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.dto.UserLoginLogoutHistoryResponseDTO;
import com.event_management_system.entity.User;
import com.event_management_system.entity.UserLoginLogoutHistory;
import com.event_management_system.mapper.UserLoginLogoutHistoryMapper;
import com.event_management_system.repository.UserLoginLogoutHistoryRepository;

/**
 * UserLoginLogoutHistoryService
 * 
 * Manages login/logout session tracking
 * 
 * Key Functions:
 * - Record login events
 * - Record logout events
 * - Track active sessions
 * - Force logout sessions
 * - Get login history
 * 
 * Used by:
 * - AuthService (when user logs in/out)
 * - HistoryController (to show login history)
 * - SecurityController (to manage active sessions)
 */
@Service
public class UserLoginLogoutHistoryService {
    
    @Autowired
    private UserLoginLogoutHistoryRepository loginHistoryRepository;
    
    @Autowired
    private UserLoginLogoutHistoryMapper loginHistoryMapper;
    
    /**
     * Record a login event
     * 
     * Usage:
     * userLoginLogoutHistoryService.recordLogin(
     *     user, jwtToken, ipAddress, deviceInfo, "SUCCESS"
     * );
     * 
     * @param user - The user who logged in
     * @param userToken - JWT token for this session
     * @param requestIp - Client IP address
     * @param deviceInfo - Browser/device information
     * @param loginStatus - "SUCCESS", "FAILED", "FORCE_LOGOUT"
     * @return Saved UserLoginLogoutHistory entity
     */
    @Transactional
    public UserLoginLogoutHistory recordLogin(
            @NonNull User user,
            @NonNull String userToken,
            @NonNull String requestIp,
            @NonNull String deviceInfo,
            @NonNull String loginStatus) {
        
        UserLoginLogoutHistory history = UserLoginLogoutHistory.builder()
                .user(user)
                .userToken(userToken)
                .userType(user.getRole() != null ? user.getRole().getName() : "UNKNOWN")
                .loginTime(LocalDateTime.now())
                .logoutTime(null)  // Not logged out yet
                .requestIp(requestIp)
                .deviceInfo(deviceInfo)
                .loginStatus(loginStatus)
                .build();
        
        UserLoginLogoutHistory saved = loginHistoryRepository.save(history);
        return Objects.requireNonNull(saved, "Saved login history should not be null");
    }
    
    /**
     * Record a logout event
     * 
     * Updates the logout time for the session with the given token
     * 
     * Usage:
     * userLoginLogoutHistoryService.recordLogout(jwtToken);
     * 
     * @param userToken - JWT token for the session
     * @return true if logout recorded, false if session not found
     */
    @Transactional
    public boolean recordLogout(@NonNull String userToken) {
        return loginHistoryRepository.findByUserToken(userToken)
                .map(history -> {
                    history.setLogoutTime(LocalDateTime.now());
                    loginHistoryRepository.save(history);
                    return true;
                })
                .orElse(false);
    }
    
    /**
     * Get all login/logout history for a user
     * 
     * Usage:
     * List<UserLoginLogoutHistoryResponseDTO> history = 
     *     userLoginLogoutHistoryService.getLoginHistory(userId);
     * 
     * @param userId - User ID
     * @return List of login/logout records (sorted newest first)
     */
    @Transactional(readOnly = true)
    public List<UserLoginLogoutHistoryResponseDTO> getLoginHistory(@NonNull Long userId) {
        List<UserLoginLogoutHistory> history = loginHistoryRepository.findByUserId(userId);
        return history.stream()
                .map(loginHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get active sessions for a user (where logoutTime is NULL)
     * 
     * Usage:
     * List<UserLoginLogoutHistoryResponseDTO> activeSessions = 
     *     userLoginLogoutHistoryService.getActiveSessions(userId);
     * 
     * @param userId - User ID
     * @return List of active sessions (not logged out yet)
     */
    @Transactional(readOnly = true)
    public List<UserLoginLogoutHistoryResponseDTO> getActiveSessions(@NonNull Long userId) {
        List<UserLoginLogoutHistory> activeSessions = 
                loginHistoryRepository.findByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(userId);
        
        return activeSessions.stream()
                .map(loginHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Count active sessions for a user
     * 
     * Usage:
     * long activeCount = userLoginLogoutHistoryService.countActiveSessions(userId);
     * 
     * @param userId - User ID
     * @return Number of active sessions
     */
    @Transactional(readOnly = true)
    public long countActiveSessions(@NonNull Long userId) {
        return loginHistoryRepository.countByUserIdAndLogoutTimeIsNull(userId);
    }
    
    /**
     * Get login history within a date range
     * 
     * Usage:
     * List<UserLoginLogoutHistoryResponseDTO> history = 
     *     userLoginLogoutHistoryService.getLoginHistoryByDateRange(
     *         userId, startDate, endDate
     *     );
     * 
     * @param userId - User ID
     * @param startDate - Start date
     * @param endDate - End date
     * @return List of login records within date range
     */
    @Transactional(readOnly = true)
    public List<UserLoginLogoutHistoryResponseDTO> getLoginHistoryByDateRange(
            @NonNull Long userId,
            @NonNull LocalDateTime startDate,
            @NonNull LocalDateTime endDate) {
        
        List<UserLoginLogoutHistory> history = loginHistoryRepository
                .findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(userId, startDate, endDate);
        
        return history.stream()
                .map(loginHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get login history by status
     * 
     * Usage:
     * List<UserLoginLogoutHistoryResponseDTO> failedLogins = 
     *     userLoginLogoutHistoryService.getLoginHistoryByStatus(userId, "FAILED");
     * 
     * @param userId - User ID
     * @param loginStatus - "SUCCESS", "FAILED", "FORCE_LOGOUT"
     * @return List of login records with specified status
     */
    @Transactional(readOnly = true)
    public List<UserLoginLogoutHistoryResponseDTO> getLoginHistoryByStatus(
            @NonNull Long userId,
            @NonNull String loginStatus) {
        
        List<UserLoginLogoutHistory> history = loginHistoryRepository
                .findByUserIdAndLoginStatusOrderByLoginTimeDesc(userId, loginStatus);
        
        return history.stream()
                .map(loginHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the most recent login for a user
     * 
     * Usage:
     * UserLoginLogoutHistoryResponseDTO lastLogin = 
     *     userLoginLogoutHistoryService.getLastLogin(userId);
     * 
     * @param userId - User ID
     * @return Most recent login record, or null if none
     */
    @Transactional(readOnly = true)
    public UserLoginLogoutHistoryResponseDTO getLastLogin(@NonNull Long userId) {
        return loginHistoryRepository.findFirstByUserIdOrderByLoginTimeDesc(userId)
                .map(loginHistoryMapper::toDto)
                .orElse(null);
    }
    
    /**
     * Force logout a session by token
     * 
     * Sets logout time to now and changes status to "FORCE_LOGOUT"
     * 
     * Usage:
     * userLoginLogoutHistoryService.forceLogout(jwtToken);
     * 
     * @param userToken - JWT token for the session
     * @return true if session found and logged out, false otherwise
     */
    @Transactional
    public boolean forceLogout(@NonNull String userToken) {
        return loginHistoryRepository.findByUserToken(userToken)
                .map(history -> {
                    history.setLogoutTime(LocalDateTime.now());
                    history.setLoginStatus("FORCE_LOGOUT");
                    loginHistoryRepository.save(history);
                    return true;
                })
                .orElse(false);
    }
}
