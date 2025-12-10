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
 * UserLoginLogoutHistory Entity
 * 
 * Records every login and logout for security and audit trail
 * 
 * Fields:
 * - user_id: Which user logged in/out
 * - user_token: The JWT token used for this session
 * - user_type: The role of the user (ADMIN, EVENT_MANAGER, etc)
 * - request_from: From which host/domain
 * - request_ip: IP address of the request
 * - device_info: Device and browser info (parsed from User-Agent)
 * - login_time: When user logged in
 * - logout_time: When user logged out (null if still logged in)
 * - login_status: SUCCESS, FAILED, FORCE_LOGOUT
 * - created_by: System user who created this record
 * - created_at: When this record was created
 * - updated_at: When this record was last updated
 * - is_active: Is this record active
 * 
 * Flow:
 * 1. User enters email/password
 * 2. System authenticates
 * 3. If successful: Record login in this table with IP, device, time
 * 4. User works...
 * 5. User logs out
 * 6. System updates logout_time in this table
 * 
 * Later: Can check "when did I login/logout?"
 * Can detect: "Unknown IP logged in - hacker alert!"
 * Can check: "Active sessions - force logout unwanted sessions"
 */
@Entity
@Table(name = "user_login_logout_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginLogoutHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The user who logged in/out
     * Foreign key to app_users table
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * The JWT token used for this login session
     * Used to track and force logout specific sessions
     */
    @Column(name = "user_token", length = 500, nullable = false)
    private String userToken;
    
    /**
     * User's role (ADMIN, EVENT_MANAGER, USER, etc)
     * Stored for quick access without joining to roles table
     */
    @Column(name = "user_type", length = 100, nullable = false)
    private String userType;
    
    /**
     * From which host/domain the request came
     * Example: "192.168.1.1" or "office.example.com"
     */
    @Column(name = "request_from", length = 100)
    private String requestFrom;
    
    /**
     * IP address of the client
     * Used to detect location changes and suspicious logins
     * Example: "192.168.1.1" or "115.42.33.55"
     */
    @Column(name = "request_ip", length = 50, nullable = false)
    private String requestIp;
    
    /**
     * Device and browser information
     * Parsed from User-Agent header
     * Example: "Windows 10 - Chrome" or "iPhone - Safari"
     */
    @Column(name = "device_info", length = 500)
    private String deviceInfo;
    
    /**
     * When the user logged in
     * Automatically set at login
     */
    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;
    
    /**
     * When the user logged out
     * NULL = Still logged in (active session)
     * Has value = Logged out
     * Used to find "active sessions"
     */
    @Column(name = "logout_time")
    private LocalDateTime logoutTime;
    
    /**
     * Which user/system created this record
     * Usually the same as 'user'
     * Foreign key to app_users table
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    /**
     * When this record was created
     * Auto-set by @PrePersist
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * When this record was last updated
     * Auto-set by @PreUpdate
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Status of the login attempt
     * Values: SUCCESS, FAILED, FORCE_LOGOUT
     * 
     * SUCCESS: User logged in successfully
     * FAILED: Login failed (wrong password, etc)
     * FORCE_LOGOUT: Admin forced logout
     */
    @Column(name = "login_status", length = 255)
    private String loginStatus;
    
    /**
     * Auto-set created_at timestamp when record is created
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
    }
    
    /**
     * Auto-set updated_at timestamp when record is updated
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
