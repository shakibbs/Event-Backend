package com.event_management_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.dto.UserActivityHistoryResponseDTO;
import com.event_management_system.entity.User;
import com.event_management_system.entity.UserActivityHistory;
import com.event_management_system.entity.UserActivityHistory.ActivityType;
import com.event_management_system.mapper.UserActivityHistoryMapper;
import com.event_management_system.repository.UserActivityHistoryRepository;

/**
 * UserActivityHistoryService
 * 
 * Manages complete activity audit trail
 * 
 * Key Functions:
 * - Record any user activity (login, logout, event created, password changed, etc.)
 * - Retrieve activity history with various filters
 * - Group activities by session
 * - Filter by activity type, date range, user group
 * 
 * Used by:
 * - AuthService (record login/logout)
 * - EventService (record event created/updated/deleted)
 * - UserService (record password changed, user created/updated/deleted)
 * - RoleService (record role assigned/revoked)
 * - HistoryController (to show activity feed)
 */
@Service
public class UserActivityHistoryService {
    
    @Autowired
    private UserActivityHistoryRepository activityHistoryRepository;
    
    @Autowired
    private UserActivityHistoryMapper activityHistoryMapper;
    
    /**
     * Record a user activity
     * 
     * Usage:
     * userActivityHistoryService.recordActivity(
     *     user, 
     *     ActivityType.EVENT_CREATED, 
     *     "Event: Spring Festival 2025",
     *     ipAddress, 
     *     deviceId, 
     *     sessionId
     * );
     * 
     * @param user - The user who performed the activity
     * @param activityType - Type of activity (from ActivityType enum)
     * @param description - Additional details (e.g., "Event: Spring Festival 2025")
     * @param ipAddress - Client IP address
     * @param deviceId - Device identifier
     * @param sessionId - Session identifier (to group related activities)
     * @return Saved UserActivityHistory entity
     */
    @Transactional
    public UserActivityHistory recordActivity(
            @NonNull User user,
            @NonNull ActivityType activityType,
            String description,
            String ipAddress,  // Nullable
            String deviceId,   // Nullable
            String sessionId) { // Nullable
        
        UserActivityHistory activity = UserActivityHistory.builder()
                .user(user)
                .username(user.getFullName())
                .userGroup(user.getRole() != null ? user.getRole().getName() : "Unknown")
                .activityTypeCode(activityType.getCode())
                .activityTypeName(activityType.getDisplayName())
                .description(description)
                .deviceId(deviceId != null ? deviceId : "unknown")
                .ip(ipAddress != null ? ipAddress : "0.0.0.0")
                .sessionId(sessionId != null ? sessionId : "")
                .activityDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now())
                .createdBy(user)  // User object, not String
                .build();
        
        UserActivityHistory saved = activityHistoryRepository.save(activity);
        return Objects.requireNonNull(saved, "Saved activity should not be null");
    }
    
    /**
     * Get all activities for a user
     * 
     * Usage:
     * List<UserActivityHistoryResponseDTO> activities = 
     *     userActivityHistoryService.getActivityHistory(userId);
     * 
     * @param userId - User ID
     * @return List of all activities (sorted newest first)
     */
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivityHistory(@NonNull Long userId) {
        List<UserActivityHistory> activities = activityHistoryRepository.findByUserId(userId);
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get activities within a date range
     * 
     * Usage:
     * List<UserActivityHistoryResponseDTO> activities = 
     *     userActivityHistoryService.getActivitiesByDateRange(
     *         userId, startDate, endDate
     *     );
     * 
     * @param userId - User ID
     * @param startDate - Start date
     * @param endDate - End date
     * @return List of activities within date range
     */
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivitiesByDateRange(
            @NonNull Long userId,
            @NonNull LocalDateTime startDate,
            @NonNull LocalDateTime endDate) {
        
        List<UserActivityHistory> activities = activityHistoryRepository
                .findByUserIdAndActivityDateBetweenOrderByActivityDateDesc(userId, startDate, endDate);
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get activities by type
     * 
     * Usage:
     * List<UserActivityHistoryResponseDTO> loginActivities = 
     *     userActivityHistoryService.getActivitiesByType("USER_LOGIN");
     * 
     * @param activityTypeCode - Activity type code (e.g., "USER_LOGIN", "EVENT_CREATED")
     * @return List of activities of specified type
     */
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivitiesByType(
            @NonNull String activityTypeCode) {
        
        List<UserActivityHistory> activities = activityHistoryRepository
                .findByActivityTypeCodeOrderByActivityDateDesc(activityTypeCode);
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get activities by session ID
     * 
     * Useful to see all activities in one login session
     * 
     * Usage:
     * List<UserActivityHistoryResponseDTO> sessionActivities = 
     *     userActivityHistoryService.getActivitiesBySession(sessionId);
     * 
     * @param sessionId - Session identifier
     * @return List of activities in the session (sorted chronologically)
     */
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivitiesBySession(
            @NonNull String sessionId) {
        
        List<UserActivityHistory> activities = activityHistoryRepository
                .findBySessionIdOrderByActivityDateAsc(sessionId);
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get activities by user group (role)
     * 
     * Usage:
     * List<UserActivityHistoryResponseDTO> adminActivities = 
     *     userActivityHistoryService.getActivitiesByUserGroup("ADMIN");
     * 
     * @param userGroup - User group/role name (e.g., "ADMIN", "USER")
     * @return List of activities by users in specified group
     */
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivitiesByUserGroup(
            @NonNull String userGroup) {
        
        List<UserActivityHistory> activities = activityHistoryRepository
                .findByUserGroupOrderByActivityDateDesc(userGroup);
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Count total activities for a user
     * 
     * Usage:
     * long activityCount = userActivityHistoryService.countActivities(userId);
     * 
     * @param userId - User ID
     * @return Total number of activities
     */
    @Transactional(readOnly = true)
    public long countActivities(@NonNull Long userId) {
        return activityHistoryRepository.countByUserId(userId);
    }
    
    /**
     * Get recent activities (last N days)
     * 
     * Usage:
     * // Get activities in last 7 days
     * List<UserActivityHistoryResponseDTO> recentActivities = 
     *     userActivityHistoryService.getRecentActivities(userId, 7);
     * 
     * @param userId - User ID
     * @param days - Number of days to look back
     * @return List of recent activities
     */
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getRecentActivities(
            @NonNull Long userId, 
            int days) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(days);
        LocalDateTime endDate = now;
        
        return getActivitiesByDateRange(
            userId,
            Objects.requireNonNull(startDate, "startDate should not be null"),
            Objects.requireNonNull(endDate, "endDate should not be null"));
    }
    
    /**
     * Get all activities (for admin view)
     * 
     * Usage:
     * List<UserActivityHistoryResponseDTO> allActivities = 
     *     userActivityHistoryService.getAllActivities();
     * 
     * @return List of all activities in the system
     */
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getAllActivities() {
        List<UserActivityHistory> activities = activityHistoryRepository
                .findByActivityDateBetweenOrderByActivityDateDesc(
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    LocalDateTime.now()
                );
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
}
