package com.event_management_system.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UserActivityHistory Entity
 * 
 * Records EVERY activity a user does in the system
 * 
 * Fields:
 * - user_id: Which user performed the activity
 * - user_group: User's role (ADMIN, EVENT_MANAGER, etc)
 * - activity_type_name: Human readable activity name (User Login, Event Created, etc)
 * - activity_type_code: Machine readable code (USER_LOGIN, EVENT_CREATED, etc)
 * - device_id: Unique device identifier
 * - username: Username for quick access
 * - activity_date: When the activity happened
 * - ip: IP address from where activity was performed
 * - session_id: Session ID for grouping activities
 * - created_by: System user who created this record
 * - created_date: When this record was created
 * - updated_by: System user who last updated this record
 * - updated_date: When this record was last updated
 * - is_active: Is this record active
 * 
 * Activity Types Examples:
 * - USER_LOGIN: User logged in
 * - USER_LOGOUT: User logged out
 * - EVENT_CREATED: User created an event
 * - EVENT_UPDATED: User updated an event
 * - EVENT_DELETED: User deleted an event
 * - PASSWORD_CHANGED: User changed password
 * - PERMISSION_GRANTED: Permission granted to user
 * - etc.
 * 
 * Flow:
 * 1. User logs in → USER_LOGIN activity recorded
 * 2. User creates event → EVENT_CREATED activity recorded
 * 3. User updates event → EVENT_UPDATED activity recorded
 * 4. User logs out → USER_LOGOUT activity recorded
 * 
 * Later: Can retrieve "All activities by user on specific date"
 * Can analyze: "What did user do during this session?"
 * Can audit: "Complete timeline of all changes"
 */
@Entity
@Table(name = "user_activity_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityHistory {
    
    /**
     * ActivityType Enum
     * 
     * Defines all possible user activities in the system
     * Nested inside UserActivityHistory entity
     * 
     * Usage:
     * UserActivityHistory.ActivityType.USER_LOGIN.getCode()      → "USER_LOGIN"
     * UserActivityHistory.ActivityType.USER_LOGIN.getDisplayName() → "User Login"
     */
    public enum ActivityType {
        
        // Authentication Activities
        USER_LOGIN("USER_LOGIN", "User Login"),
        USER_LOGOUT("USER_LOGOUT", "User Logout"),
        
        // Password Management
        PASSWORD_CHANGED("PASSWORD_CHANGED", "Password Changed"),
        PASSWORD_RESET("PASSWORD_RESET", "Password Reset"),
        PASSWORD_ATTEMPT_FAILED("PASSWORD_ATTEMPT_FAILED", "Password Attempt Failed"),
        
        // Event Management
        EVENT_CREATED("EVENT_CREATED", "Event Created"),
        EVENT_UPDATED("EVENT_UPDATED", "Event Updated"),
        EVENT_DELETED("EVENT_DELETED", "Event Deleted"),
        
        // Role & Permission Management
        ROLE_ASSIGNED("ROLE_ASSIGNED", "Role Assigned"),
        ROLE_REVOKED("ROLE_REVOKED", "Role Revoked"),
        PERMISSION_GRANTED("PERMISSION_GRANTED", "Permission Granted"),
        PERMISSION_REVOKED("PERMISSION_REVOKED", "Permission Revoked"),
        
        // User Management Activities
        USER_CREATED("USER_CREATED", "User Created"),
        USER_UPDATED("USER_UPDATED", "User Updated"),
        USER_DELETED("USER_DELETED", "User Deleted");
        
        private final String code;
        private final String displayName;
        
        /**
         * Constructor for ActivityType
         * 
         * @param code - Machine-readable code (stored in database)
         * @param displayName - Human-readable name (shown in UI)
         */
        ActivityType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        /**
         * Get the code (what's stored in database)
         * 
         * @return code like "USER_LOGIN"
         */
        public String getCode() {
            return code;
        }
        
        /**
         * Get the display name (what's shown to users)
         * 
         * @return displayName like "User Login"
         */
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * Convert string code to ActivityType enum
         * 
         * Usage:
         * ActivityType type = ActivityType.fromCode("USER_LOGIN");
         * 
         * @param code - The code to find
         * @return ActivityType matching the code
         * @throws IllegalArgumentException if code not found
         */
        public static ActivityType fromCode(String code) {
            if (code == null) {
                throw new IllegalArgumentException("Activity code cannot be null");
            }
            
            for (ActivityType type : ActivityType.values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
            
            throw new IllegalArgumentException("Unknown activity code: " + code);
        }
        
        /**
         * Check if a code is valid
         * 
         * @param code - The code to check
         * @return true if valid, false otherwise
         */
        public static boolean isValid(String code) {
            try {
                fromCode(code);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The user who performed this activity
     * Foreign key to app_users table
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * User's role/group (stored for quick access)
     * Example: "ADMIN", "EVENT_MANAGER", "USER"
     * Avoids joining to roles table
     */
    @Column(name = "user_group", length = 255)
    private String userGroup;
    
    /**
     * Human readable activity name
     * Example: "User Login", "Event Created", "Password Changed"
     * Used for UI display
     */
    @Column(name = "activity_type_name", length = 255)
    private String activityTypeName;
    
    /**
     * Machine readable activity code
     * Example: "USER_LOGIN", "EVENT_CREATED", "PASSWORD_CHANGED"
     * Used for filtering and analysis
     */
    @Column(name = "activity_type_code", length = 255)
    private String activityTypeCode;
    
    /**
     * Additional description with specific details about the activity
     * Example: "Event: Spring Festival 2025", "Changed email from old@email.com to new@email.com"
     * Provides context about what exactly was changed/created/deleted
     */
    @Column(name = "description", length = 1000)
    private String description;
    
    /**
     * Unique device identifier
     * Generated from User-Agent + IP hash
     * Used to track which device performed activity
     */
    @Column(name = "device_id", length = 255)
    private String deviceId;
    
    /**
     * Username (stored for quick access)
     * Avoids joining to users table
     * Example: "Shakib", "Admin", "John"
     */
    @Column(name = "username", length = 255)
    private String username;
    
    /**
     * Which user/system created this record
     * Usually the same as 'user'
     * Foreign key to app_users table
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    /**
     * When the activity happened
     * This is THE important timestamp for activity tracking
     * Example: "2025-12-08 10:15:30"
     */
    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;
    
    /**
     * When this record was created in database
     * Auto-set by @PrePersist
     * Usually same as activity_date
     */
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    /**
     * Which user/system last updated this record
     * Useful if activity details are corrected later
     * Foreign key to app_users table
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
    
    /**
     * When this record was last updated
     * Auto-set by @PreUpdate
     */
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
    
    /**
     * IP address from where activity was performed
     * Used to detect suspicious activities
     * Example: "192.168.1.1" or "115.42.33.55"
     */
    @Column(name = "ip", length = 50)
    private String ip;
    
    /**
     * Session ID
     * Groups all activities from same login session
     * Used to track "What did user do in this session?"
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;
    
    /**
     * Auto-set created_date timestamp when record is created
     */
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
        if (activityDate == null) {
            activityDate = LocalDateTime.now();
        }
    }
    
    /**
     * Auto-set updated_date timestamp when record is updated
     */
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
