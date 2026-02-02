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

import com.event_management_system.dto.AllHistoryResponseDTO;
import com.event_management_system.dto.UserActivityHistoryResponseDTO;
import com.event_management_system.dto.UserLoginLogoutHistoryResponseDTO;
import com.event_management_system.dto.UserPasswordHistoryResponseDTO;
import com.event_management_system.service.ApplicationLoggerService;
import com.event_management_system.service.UserActivityHistoryService;
import com.event_management_system.service.UserLoginLogoutHistoryService;
import com.event_management_system.service.UserPasswordHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/history")
@Tag(name = "History", description = "APIs for retrieving user history and activities")
public class HistoryController {
        @Autowired
        private com.event_management_system.service.ReportService reportService;
    
    @Autowired
    private UserLoginLogoutHistoryService loginHistoryService;
    
    @Autowired
    private UserPasswordHistoryService passwordHistoryService;
    
    @Autowired
    private UserActivityHistoryService activityHistoryService;
    
    @Autowired
    private com.event_management_system.service.UserService userService;
    
    @Autowired
    private ApplicationLoggerService log;
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        org.springframework.security.core.userdetails.User userDetails =
            (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
        
        return userService.getUserIdByEmail(userDetails.getUsername());
    }
    
    private boolean canViewUserHistory(Long targetUserId) {
        Long currentUserId = getCurrentUserId();
        Objects.requireNonNull(currentUserId, "Current user ID should not be null");
        
        if (userService.hasPermission(currentUserId, "history.view.all")) {
            return true;
        }
        
        if (!currentUserId.equals(targetUserId)) {
            throw new RuntimeException("You don't have permission to view this user's history");
        }
        
        return true;
    }
    
    
    
    @GetMapping("/login")
    @Operation(
        summary = "Get login/logout history",
        description = "Retrieves all login and logout events for the authenticated user, a specific user (SuperAdmin only), or all users (SuperAdmin only)"
    )
    public ResponseEntity<List<UserLoginLogoutHistoryResponseDTO>> getLoginHistory(
            @Parameter(description = "Target user ID (SuperAdmin only)")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "All users (SuperAdmin only)")
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        try {
            Long currentUserId = getCurrentUserId();
            boolean isSuperAdmin = userService.hasPermission(currentUserId, "history.view.all");
            if (all && isSuperAdmin) {
                log.info("[HistoryController] INFO - getLoginHistory() - Fetching ALL login/logout history (SuperAdmin)");
                List<UserLoginLogoutHistoryResponseDTO> history = loginHistoryService.getAllLoginHistory();
                return new ResponseEntity<>(history, HttpStatus.OK);
            }
            Long targetUserId = (userId != null) ? userId : currentUserId;
            Objects.requireNonNull(targetUserId, "Target user ID should not be null");
            canViewUserHistory(targetUserId);
            log.info("[HistoryController] INFO - getLoginHistory() - Fetching login history for user: {}", targetUserId);
            List<UserLoginLogoutHistoryResponseDTO> history = loginHistoryService.getLoginHistory(targetUserId);
            return new ResponseEntity<>(history, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("[HistoryController] ERROR - getLoginHistory() - Failed to fetch login history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getLoginHistory() - Failed to fetch login history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
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
            
            log.info("[HistoryController] INFO - getLoginHistoryByDateRange() - Fetching login history for user {} from {} to {}", userId, startDate, endDate);
            
            List<UserLoginLogoutHistoryResponseDTO> history = 
                loginHistoryService.getLoginHistoryByDateRange(userId, startDate, endDate);
            
            return new ResponseEntity<>(history, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getLoginHistoryByDateRange() - Failed to fetch login history by date range: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @GetMapping("/login/failed")
    @Operation(
        summary = "Get failed login attempts",
        description = "Retrieves all failed login attempts for security monitoring"
    )
    public ResponseEntity<List<UserLoginLogoutHistoryResponseDTO>> getFailedLogins() {
        try {
            Long userId = getCurrentUserId();
            Objects.requireNonNull(userId, "User ID should not be null");
            
            log.info("[HistoryController] INFO - getFailedLogins() - Fetching failed login attempts for user: {}", userId);
            
            List<UserLoginLogoutHistoryResponseDTO> failedLogins = 
                loginHistoryService.getLoginHistoryByStatus(userId, "FAILED");
            
            return new ResponseEntity<>(failedLogins, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getFailedLogins() - Failed to fetch failed login attempts: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    
    @GetMapping("/password")
    @Operation(
        summary = "Get password change history",
        description = "Retrieves all password changes for the authenticated user, a specific user (SuperAdmin only), or all users (SuperAdmin only) - passwords are never exposed"
    )
    public ResponseEntity<List<UserPasswordHistoryResponseDTO>> getPasswordHistory(
            @Parameter(description = "Target user ID (SuperAdmin only)")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "All users (SuperAdmin only)")
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        try {
            Long currentUserId = getCurrentUserId();
            boolean isSuperAdmin = userService.hasPermission(currentUserId, "history.view.all");
            if (all && isSuperAdmin) {
                log.info("[HistoryController] INFO - getPasswordHistory() - Fetching ALL password change history (SuperAdmin)");
                List<UserPasswordHistoryResponseDTO> history = passwordHistoryService.getAllPasswordHistory();
                return new ResponseEntity<>(history, HttpStatus.OK);
            }
            Long targetUserId = (userId != null) ? userId : currentUserId;
            Objects.requireNonNull(targetUserId, "Target user ID should not be null");
            canViewUserHistory(targetUserId);
            log.info("[HistoryController] INFO - getPasswordHistory() - Fetching password history for user: {}", targetUserId);
            List<UserPasswordHistoryResponseDTO> history = passwordHistoryService.getPasswordHistory(targetUserId);
            return new ResponseEntity<>(history, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("[HistoryController] ERROR - getPasswordHistory() - Failed to fetch password history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getPasswordHistory() - Failed to fetch password history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    
    @GetMapping("/activity")
    @Operation(
        summary = "Get activity history",
        description = "Retrieves all activities for the authenticated user, a specific user (SuperAdmin only), or all users (SuperAdmin only)"
    )
    public ResponseEntity<List<UserActivityHistoryResponseDTO>> getActivityHistory(
            @Parameter(description = "Target user ID (SuperAdmin only)")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "All users (SuperAdmin only)")
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        try {
            Long currentUserId = getCurrentUserId();
            boolean isSuperAdmin = userService.hasPermission(currentUserId, "history.view.all");

            if (all && isSuperAdmin) {
                log.info("[HistoryController] INFO - getActivityHistory() - Fetching ALL activity history (SuperAdmin)");
                List<UserActivityHistoryResponseDTO> activities = activityHistoryService.getAllActivityHistory();
                return new ResponseEntity<>(activities, HttpStatus.OK);
            }

            Long targetUserId = (userId != null) ? userId : currentUserId;
            Objects.requireNonNull(targetUserId, "Target user ID should not be null");
            canViewUserHistory(targetUserId);

            log.info("[HistoryController] INFO - getActivityHistory() - Fetching activity history for user: {}", targetUserId);
            List<UserActivityHistoryResponseDTO> activities = activityHistoryService.getActivityHistory(targetUserId);
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (RuntimeException e) {
            log.error("[HistoryController] ERROR - getActivityHistory() - Failed to fetch activity history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getActivityHistory() - Failed to fetch activity history: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
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
            
            log.info("[HistoryController] INFO - getRecentActivities() - Fetching recent activities for user {} in last {} days", userId, days);
            
            List<UserActivityHistoryResponseDTO> activities = 
                activityHistoryService.getRecentActivities(userId, days);
            
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getRecentActivities() - Failed to fetch recent activities: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
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
            
            log.info("[HistoryController] INFO - getActivitiesByType() - Fetching activities of type: {}", type);
            
            List<UserActivityHistoryResponseDTO> activities = 
                activityHistoryService.getActivitiesByType(type);
            
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getActivitiesByType() - Failed to fetch activities by type: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
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
            
            log.info("[HistoryController] INFO - getActivitiesByDateRange() - Fetching activities for user {} from {} to {}", userId, startDate, endDate);
            
            List<UserActivityHistoryResponseDTO> activities = 
                activityHistoryService.getActivitiesByDateRange(userId, startDate, endDate);
            
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getActivitiesByDateRange() - Failed to fetch activities by date range: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
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
            
            log.info("[HistoryController] INFO - getActivitiesBySession() - Fetching activities for session: {}", sessionId);
            
            List<UserActivityHistoryResponseDTO> activities = 
                activityHistoryService.getActivitiesBySession(sessionId);
            
            return new ResponseEntity<>(activities, HttpStatus.OK);
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getActivitiesBySession() - Failed to fetch activities by session: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


        @GetMapping("/all")
    @Operation(
        summary = "Get all history types (activity, login/logout, password change)",
        description = "Fetches all history types for the authenticated user, a specific user (SuperAdmin only), or all users (SuperAdmin only)"
    )
    public ResponseEntity<AllHistoryResponseDTO> getAllHistory(
            @Parameter(description = "Target user ID (SuperAdmin only)")
            @RequestParam(required = false) Long userId,
            @Parameter(description = "All users (SuperAdmin only)")
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        try {
            Long currentUserId = getCurrentUserId();
            boolean isSuperAdmin = userService.hasPermission(currentUserId, "history.view.all");
            Long targetUserId = (userId != null) ? userId : currentUserId;
            if (!isSuperAdmin && (all || !currentUserId.equals(targetUserId))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<UserActivityHistoryResponseDTO> activities = isSuperAdmin && all
                ? activityHistoryService.getAllActivityHistory()
                : activityHistoryService.getActivityHistory(targetUserId);

            List<UserLoginLogoutHistoryResponseDTO> logins = isSuperAdmin && all
                ? loginHistoryService.getAllLoginHistory()
                : loginHistoryService.getLoginHistory(targetUserId);

            List<UserPasswordHistoryResponseDTO> passwords = isSuperAdmin && all
                ? passwordHistoryService.getAllPasswordHistory()
                : passwordHistoryService.getPasswordHistory(targetUserId);

            AllHistoryResponseDTO response = new AllHistoryResponseDTO(activities, logins, passwords);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("[HistoryController] ERROR - getAllHistory() - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("[HistoryController] ERROR - getAllHistory() - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
        @GetMapping(value = "/download/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
        @Operation(summary = "Download history as PDF", description = "Downloads a PDF of user activity, login/logout, or password history, filtered by type and role/permission.")
        public ResponseEntity<byte[]> downloadHistoryPdf(
                Authentication authentication,
                @RequestParam(name = "type", defaultValue = "activity") String type) {
            log.info("PDF history export endpoint called. Authentication: {}, type: {}", authentication, type);
            if (authentication == null) {
                log.warn("No authentication provided to /api/history/download/pdf");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String email = authentication.getName();
            Long currentUserId = userService.getUserIdByEmail(email);

            // Permission logic: .view.all, .view.own, .export for each type
            String permAll, permOwn, permExport;
            java.util.List<?> allData;
            java.util.List<?> filteredData;
            String filename;
            switch (type.toLowerCase()) {
                case "login":
                case "loginlogout":
                case "login-logout":
                    permAll = "loginhistory.view.all";
                    permOwn = "loginhistory.view.own";
                    permExport = "loginhistory.export";
                    allData = loginHistoryService.getAllLoginHistory();
                    filename = "login_logout_history.pdf";
                    break;
                case "password":
                case "passwordhistory":
                    permAll = "passwordhistory.view.all";
                    permOwn = "passwordhistory.view.own";
                    permExport = "passwordhistory.export";
                    allData = passwordHistoryService.getAllPasswordHistory();
                    filename = "password_history.pdf";
                    break;
                case "activity":
                default:
                    permAll = "history.view.all";
                    permOwn = "history.view.own";
                    permExport = "history.export";
                    allData = activityHistoryService.getAllActivityHistory();
                    filename = "activity_history.pdf";
                    break;
            }

            boolean canExportAll = userService.hasPermission(currentUserId, permAll);
            boolean canExportOwn = userService.hasPermission(currentUserId, permOwn);
            boolean canExport = userService.hasPermission(currentUserId, permExport) || canExportAll || canExportOwn;
            if (!canExport) {
                log.warn("User {} does not have permission to export {} history", currentUserId, type);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (canExportAll) {
                filteredData = allData;
            } else if (canExportOwn) {
                @SuppressWarnings("unchecked")
                java.util.List<Object> allDataObj = (java.util.List<Object>) allData;
                filteredData = allDataObj.stream()
                    .filter(obj -> {
                        try {
                            java.lang.reflect.Method getUserId = obj.getClass().getMethod("getUserId");
                            Object uid = getUserId.invoke(obj);
                            return uid != null && uid.equals(currentUserId);
                        } catch (ReflectiveOperationException e) {
                            return false;
                        }
                    })
                    .collect(java.util.stream.Collectors.toList());
            } else {
                filteredData = java.util.Collections.emptyList();
            }

            java.util.Map<String, Object> parameters = new java.util.HashMap<>();
            parameters.put("generatedBy", email);
            parameters.put("generatedAt", java.time.LocalDateTime.now().toString());

            try {
                byte[] pdfBytes;
                if ("activity".equalsIgnoreCase(type)) {
                    pdfBytes = reportService.generateActivityPdf(filteredData, parameters);
                } else if ("login".equalsIgnoreCase(type) || "loginlogout".equalsIgnoreCase(type) || "login-logout".equalsIgnoreCase(type)) {
                    pdfBytes = reportService.generateEventsPdf(filteredData, parameters); // fallback for login/logout, or implement a specific method if needed
                } else if ("password".equalsIgnoreCase(type) || "passwordhistory".equalsIgnoreCase(type)) {
                    pdfBytes = reportService.generateEventsPdf(filteredData, parameters); // fallback for password, or implement a specific method if needed
                } else {
                    pdfBytes = reportService.generateActivityPdf(filteredData, parameters);
                }
                return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=" + filename)
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
            } catch (net.sf.jasperreports.engine.JRException | IllegalArgumentException e) {
                log.error("Failed to generate PDF report", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    
}
