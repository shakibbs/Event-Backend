package com.event_management_system.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.event_management_system.dto.UserActivityHistoryResponseDTO;
import com.event_management_system.dto.UserLoginLogoutHistoryResponseDTO;
import com.event_management_system.dto.UserPasswordHistoryResponseDTO;
import com.event_management_system.service.UserActivityHistoryService;
import com.event_management_system.service.UserLoginLogoutHistoryService;
import com.event_management_system.service.UserPasswordHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * HistoryController
 * 
 * Provides API endpoints for retrieving user history:
 * - Login/Logout history
 * - Password change history
 * - Activity history
 * 
 * SECURITY:
 * - All endpoints require authentication
 * - SuperAdmin (history.view.all permission): Can view ANY user's history via userId parameter
 * - Admin/Attendee (history.view.own permission): Can ONLY view their own history
 * - Passwords are never exposed in responses
 * 
 * ENDPOINTS:
 * 1. GET /api/history/login - Get login/logout history
 * 2. GET /api/history/login/active - Get active sessions
 * 3. GET /api/history/password - Get password change history
 * 4. GET /api/history/activity - Get all activities
 * 5. GET /api/history/activity/recent - Get recent activities
 * 6. GET /api/history/activity/type/{type} - Get activities by type
 */
@Slf4j
@RestController
@RequestMapping("/api/history")
@Tag(name = "History", description = "APIs for retrieving user history and activities")
public class HistoryController {
    
    @Autowired
    private UserLoginLogoutHistoryService loginHistoryService;
    
    @Autowired
    private UserPasswordHistoryService passwordHistoryService;
    
    @Autowired
    private UserActivityHistoryService activityHistoryService;
    
    @Autowired
    private com.event_management_system.service.UserService userService;
    
    /**
     * Helper method: Get current authenticated user ID
     *
     * @return User ID from security context
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        // Get user details from authentication principal
        org.springframework.security.core.userdetails.User userDetails =
            (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        
        // Extract user ID from username (which contains email) by querying the user service
        return userService.getUserIdByEmail(userDetails.getUsername());
    }
    
    /**
     * Helper method: Check if current user can view target user's history
     * 
     * Rules:
     * - SuperAdmin (history.view.all): Can view ANY user's history
     * - Admin/Attendee (history.view.own): Can ONLY view their OWN history
     * 
     * @param targetUserId - The user ID whose history is being requested
     * @return true if allowed, throws exception if not allowed
     */
    private boolean canViewUserHistory(Long targetUserId) {
        Long currentUserId = getCurrentUserId();
        Objects.requireNonNull(currentUserId, "Current user ID should not be null");
        
        // SuperAdmin can view anyone's history
        if (userService.hasPermission(currentUserId, "history.view.all")) {
            return true;
        }
        
        // Others can only view their own history
        if (!currentUserId.equals(targetUserId)) {
            throw new RuntimeException("You don't have permission to view this user's history");
        }
        
        return true;
    }
    
    // ==================== LOGIN/LOGOUT HISTORY ENDPOINTS ====================
    
    /**
     * Get login/logout history for current user or specified user (SuperAdmin only)
     * 
     * Usage:
     * GET /api/history/login - Get own history
     * GET /api/history/login?userId=5 - SuperAdmin gets user 5's history
     * 
     * Query Parameters:
     * - userId: Optional - Target user ID (SuperAdmin only)
     * 
     * Response:
     * [
     *   {
     *     "id": 1,
     *     "loginTime": "2025-12-09T10:00:00",
     *     "logoutTime": "2025-12-09T11:00:00",
     *     "requestIp": "192.168.1.100",
     *     "deviceInfo": "Chrome 120 on Windows 10 (Desktop)",
     *     "loginStatus": "SUCCESS",
     *     "isActiveSession": false
     *   },
     *   {
     *     "id": 2,
     *     "loginTime": "2025-12-09T12:00:00",
     *     "logoutTime": null,
     *     "requestIp": "192.168.1.100",
     *     "deviceInfo": "Chrome 120 on Windows 10 (Desktop)",
     *     "loginStatus": "SUCCESS",
     *     "isActiveSession": true  ← Current session
     *   }
     * ]
     */
    @GetMapping("/login")
    @Operation(
        summary = "Get login/logout history",
        description = "Retrieves all login and logout events for the authenticated user or specified user (SuperAdmin only)"
    )
    public ResponseEntity<List<UserLoginLogoutHistoryResponseDTO>> getLoginHistory(
            @Parameter(description = "Target user ID (SuperAdmin only)")
            @RequestParam(required = false) Long userId) {
        try {
            // If userId not provided, use current user
            Long targetUserId = (userId != null) ? userId : getCurrentUserId();
            Objects.requireNonNull(targetUserId, "Target user ID should not be null");
            
            // Check permission
            canViewUserHistory(targetUserId);
            
            log.info("Fetching login history for user: {}", targetUserId);
            
            List<UserLoginLogoutHistoryResponseDTO> history = loginHistoryService.getLoginHistory(targetUserId);
            
            return new ResponseEntity<>(history, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Failed to fetch login history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Failed to fetch login history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get active sessions for current user
     * 
     * Usage:
     * GET /api/history/login/active
     * 
     * Response:
     * [
     *   {
     *     "id": 2,
     *     "loginTime": "2025-12-09T12:00:00",
     *     "logoutTime": null,
     *     "requestIp": "192.168.1.100",
     *     "deviceInfo": "Chrome 120 on Windows 10 (Desktop)",
     *     "isActiveSession": true
     *   },
     *   {
     *     "id": 3,
     *     "loginTime": "2025-12-09T11:30:00",
     *     "logoutTime": null,
     *     "requestIp": "203.54.89.20",
     *     "deviceInfo": "Safari on iPhone (Mobile)",
     *     "isActiveSession": true
     *   }
     * ]
     */
    @GetMapping("/login/active")
    @Operation(
        summary = "Get active sessions",
        description = "Retrieves all currently active login sessions (where logout_time is NULL)"
    )
    public ResponseEntity<List<UserLoginLogoutHistoryResponseDTO>> getActiveSessions(
            @Parameter(description = "Target user ID (SuperAdmin only)")
            @RequestParam(required = false) Long userId) {
        try {
            Long targetUserId = Objects.requireNonNull(
                (userId != null) ? userId : getCurrentUserId(),
                "Target user ID should not be null");
            canViewUserHistory(targetUserId);
            
            log.info("Fetching active sessions for user: {}", targetUserId);
            
            List<UserLoginLogoutHistoryResponseDTO> activeSessions = loginHistoryService.getActiveSessions(targetUserId);
            
            return new ResponseEntity<>(activeSessions, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Failed to fetch active sessions: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Failed to fetch active sessions: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get login history within date range
     * 
     * Usage:
     * GET /api/history/login/range?startDate=2025-12-01T00:00:00&endDate=2025-12-09T23:59:59
     * 
     * Query Parameters:
     * - startDate: Start date (ISO format: yyyy-MM-dd'T'HH:mm:ss)
     * - endDate: End date (ISO format: yyyy-MM-dd'T'HH:mm:ss)
     */
    @GetMapping("/login/range")
    @Operation(
        summary = "Get login history by date range",
        description = "Retrieves login history within a specified date range"
    )
    public ResponseEntity<List<UserLoginLogoutHistoryResponseDTO>> getLoginHistoryByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            Long userId = getCurrentUserId();
            Objects.requireNonNull(userId, "User ID should not be null");
            Objects.requireNonNull(startDate, "Start date should not be null");
            Objects.requireNonNull(endDate, "End date should not be null");
            
            log.info("Fetching login history for user {} from {} to {}", userId, startDate, endDate);
            
            List<UserLoginLogoutHistoryResponseDTO> history = 
                loginHistoryService.getLoginHistoryByDateRange(userId, startDate, endDate);
            
            return new ResponseEntity<>(history, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch login history by date range: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get failed login attempts
     * 
     * Usage:
     * GET /api/history/login/failed
     * 
     * Response: List of failed login attempts (security monitoring)
     */
    @GetMapping("/login/failed")
    @Operation(
        summary = "Get failed login attempts",
        description = "Retrieves all failed login attempts for security monitoring"
    )
    public ResponseEntity<List<UserLoginLogoutHistoryResponseDTO>> getFailedLogins() {
        try {
            Long userId = getCurrentUserId();
            Objects.requireNonNull(userId, "User ID should not be null");
            
            log.info("Fetching failed login attempts for user: {}", userId);
            
            List<UserLoginLogoutHistoryResponseDTO> failedLogins = 
                loginHistoryService.getLoginHistoryByStatus(userId, "FAILED");
            
            return new ResponseEntity<>(failedLogins, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch failed login attempts: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // ==================== PASSWORD HISTORY ENDPOINTS ====================
    
    /**
     * Get password change history for current user
     * 
     * Usage:
     * GET /api/history/password
     * 
     * Response:
     * [
     *   {
     *     "id": 1,
     *     "userId": 5,
     *     "userFullName": "John Doe",
     *     "passwordChangedById": 5,
     *     "passwordChangedByName": "John Doe",  ← Changed by self
     *     "changeDate": "2025-11-09T10:00:00",
     *     "createdDate": "2025-11-09T10:00:00"
     *   },
     *   {
     *     "id": 2,
     *     "passwordChangedById": 1,
     *     "passwordChangedByName": "Admin User",  ← Changed by admin
     *     "changeDate": "2025-12-09T14:00:00"
     *   }
     * ]
     * 
     * Note: Password hashes are NEVER included in response for security
     */
    @GetMapping("/password")
    @Operation(
        summary = "Get password change history",
        description = "Retrieves all password changes for the authenticated user or specified user (SuperAdmin only) - passwords are never exposed"
    )
    public ResponseEntity<List<UserPasswordHistoryResponseDTO>> getPasswordHistory(
            @Parameter(description = "Target user ID (SuperAdmin only)")
            @RequestParam(required = false) Long userId) {
        try {
            Long targetUserId = (userId != null) ? userId : getCurrentUserId();
            Objects.requireNonNull(targetUserId, "Target user ID should not be null");
            
            canViewUserHistory(targetUserId);
            
            log.info("Fetching password history for user: {}", targetUserId);
            
            List<UserPasswordHistoryResponseDTO> history = passwordHistoryService.getPasswordHistory(targetUserId);
            
            return new ResponseEntity<>(history, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Failed to fetch password history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Failed to fetch password history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get recent password changes
     * 
     * Usage:
     * GET /api/history/password/recent?days=30
     * 
     * Query Parameters:
     * - days: Number of days to look back (default: 30)
     */
    @GetMapping("/password/recent")
    @Operation(
        summary = "Get recent password changes",
        description = "Retrieves password changes within the last N days"
    )
    public ResponseEntity<List<UserPasswordHistoryResponseDTO>> getRecentPasswordChanges(
            @Parameter(description = "Number of days to look back")
            @RequestParam(defaultValue = "30") int days) {
        
        try {
            Long userId = getCurrentUserId();
            Objects.requireNonNull(userId, "User ID should not be null");
            
            log.info("Fetching password changes for user {} in last {} days", userId, days);
            
            List<UserPasswordHistoryResponseDTO> history = 
                passwordHistoryService.getRecentPasswordChanges(userId, days);
            
            return new ResponseEntity<>(history, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch recent password changes: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    // ==================== ACTIVITY HISTORY ENDPOINTS ====================
    
    /**
     * Get all activities for current user
     * 
     * Usage:
     * GET /api/history/activity
     * 
     * Response:
     * [
     *   {
     *     "id": 1,
     *     "activityTypeCode": "USER_LOGIN",
     *     "activityTypeName": "User Login",
     *     "description": "Logged in from Chrome 120 on Windows 10",
     *     "activityDate": "2025-12-09T10:00:00",
     *     "ip": "192.168.1.100",
     *     "deviceId": "device_abc123"
     *   },
     *   {
     *     "activityTypeCode": "EVENT_CREATED",
     *     "activityTypeName": "Event Created",
     *     "description": "Event: Spring Festival 2025",
     *     "activityDate": "2025-12-09T10:30:00"
     *   }
     * ]
     */
    @GetMapping("/activity")
    @Operation(
        summary = "Get activity history",
        description = "Retrieves all activities for the authenticated user or specified user (SuperAdmin only)"
    )
    public ResponseEntity<List<UserActivityHistoryResponseDTO>> getActivityHistory(
            @Parameter(description = "Target user ID (SuperAdmin only)")
            @RequestParam(required = false) Long userId) {
        try {
            Long targetUserId = (userId != null) ? userId : getCurrentUserId();
            Objects.requireNonNull(targetUserId, "Target user ID should not be null");
            
            canViewUserHistory(targetUserId);
            
            log.info("Fetching activity history for user: {}", targetUserId);
            
            List<UserActivityHistoryResponseDTO> activities = activityHistoryService.getActivityHistory(targetUserId);
            
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("Failed to fetch activity history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("Failed to fetch activity history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get recent activities
     * 
     * Usage:
     * GET /api/history/activity/recent?days=7
     * 
     * Query Parameters:
     * - days: Number of days to look back (default: 7)
     */
    @GetMapping("/activity/recent")
    @Operation(
        summary = "Get recent activities",
        description = "Retrieves activities within the last N days"
    )
    public ResponseEntity<List<UserActivityHistoryResponseDTO>> getRecentActivities(
            @Parameter(description = "Number of days to look back")
            @RequestParam(defaultValue = "7") int days) {
        
        try {
            Long userId = getCurrentUserId();
            Objects.requireNonNull(userId, "User ID should not be null");
            
            log.info("Fetching recent activities for user {} in last {} days", userId, days);
            
            List<UserActivityHistoryResponseDTO> activities = 
                activityHistoryService.getRecentActivities(userId, days);
            
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch recent activities: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get activities by type
     * 
     * Usage:
     * GET /api/history/activity/type/USER_LOGIN
     * GET /api/history/activity/type/EVENT_CREATED
     * 
     * Path Parameters:
     * - type: Activity type code (USER_LOGIN, EVENT_CREATED, PASSWORD_CHANGED, etc.)
     */
    @GetMapping("/activity/type/{type}")
    @Operation(
        summary = "Get activities by type",
        description = "Retrieves activities filtered by activity type (USER_LOGIN, EVENT_CREATED, etc.)"
    )
    public ResponseEntity<List<UserActivityHistoryResponseDTO>> getActivitiesByType(
            @Parameter(description = "Activity type code")
            @PathVariable String type) {
        
        try {
            Objects.requireNonNull(type, "Activity type should not be null");
            
            log.info("Fetching activities of type: {}", type);
            
            List<UserActivityHistoryResponseDTO> activities = 
                activityHistoryService.getActivitiesByType(type);
            
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch activities by type: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get activities by date range
     * 
     * Usage:
     * GET /api/history/activity/range?startDate=2025-12-01T00:00:00&endDate=2025-12-09T23:59:59
     * 
     * Query Parameters:
     * - startDate: Start date (ISO format: yyyy-MM-dd'T'HH:mm:ss)
     * - endDate: End date (ISO format: yyyy-MM-dd'T'HH:mm:ss)
     */
    @GetMapping("/activity/range")
    @Operation(
        summary = "Get activities by date range",
        description = "Retrieves activities within a specified date range"
    )
    public ResponseEntity<List<UserActivityHistoryResponseDTO>> getActivitiesByDateRange(
            @Parameter(description = "Start date (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "End date (yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        try {
            Long userId = getCurrentUserId();
            Objects.requireNonNull(userId, "User ID should not be null");
            Objects.requireNonNull(startDate, "Start date should not be null");
            Objects.requireNonNull(endDate, "End date should not be null");
            
            log.info("Fetching activities for user {} from {} to {}", userId, startDate, endDate);
            
            List<UserActivityHistoryResponseDTO> activities = 
                activityHistoryService.getActivitiesByDateRange(userId, startDate, endDate);
            
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch activities by date range: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Get activities by session
     * 
     * Usage:
     * GET /api/history/activity/session/{sessionId}
     * 
     * Path Parameters:
     * - sessionId: Session identifier (token UUID)
     * 
     * Use Case: See all activities performed in one login session
     */
    @GetMapping("/activity/session/{sessionId}")
    @Operation(
        summary = "Get activities by session",
        description = "Retrieves all activities performed in a specific login session"
    )
    public ResponseEntity<List<UserActivityHistoryResponseDTO>> getActivitiesBySession(
            @Parameter(description = "Session ID (token UUID)")
            @PathVariable String sessionId) {
        
        try {
            Objects.requireNonNull(sessionId, "Session ID should not be null");
            
            log.info("Fetching activities for session: {}", sessionId);
            
            List<UserActivityHistoryResponseDTO> activities = 
                activityHistoryService.getActivitiesBySession(sessionId);
            
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to fetch activities by session: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
