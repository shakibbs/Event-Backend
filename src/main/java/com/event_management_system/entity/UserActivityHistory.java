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

@Entity
@Table(name = "user_activity_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserActivityHistory {
    
    public enum ActivityType {
        USER_LOGIN("USER_LOGIN", "User Login"),
        USER_LOGOUT("USER_LOGOUT", "User Logout"),
        PASSWORD_CHANGED("PASSWORD_CHANGED", "Password Changed"),
        PASSWORD_RESET("PASSWORD_RESET", "Password Reset"),
        PASSWORD_ATTEMPT_FAILED("PASSWORD_ATTEMPT_FAILED", "Password Attempt Failed"),
        EVENT_CREATED("EVENT_CREATED", "Event Created"),
        EVENT_UPDATED("EVENT_UPDATED", "Event Updated"),
        EVENT_DELETED("EVENT_DELETED", "Event Deleted"),
        ROLE_ASSIGNED("ROLE_ASSIGNED", "Role Assigned"),
        ROLE_REVOKED("ROLE_REVOKED", "Role Revoked"),
        PERMISSION_GRANTED("PERMISSION_GRANTED", "Permission Granted"),
        PERMISSION_REVOKED("PERMISSION_REVOKED", "Permission Revoked"),
        USER_CREATED("USER_CREATED", "User Created"),
        USER_UPDATED("USER_UPDATED", "User Updated"),
        USER_DELETED("USER_DELETED", "User Deleted");
        
        private final String code;
        private final String displayName;
        
        ActivityType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
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
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "user_group", length = 255)
    private String userGroup;
    
    @Column(name = "activity_type_name", length = 255)
    private String activityTypeName;
    
    @Column(name = "activity_type_code", length = 255)
    private String activityTypeCode;
    
    @Column(name = "description", length = 1000)
    private String description;
    
    @Column(name = "device_id", length = 255)
    private String deviceId;
    
    @Column(name = "username", length = 255)
    private String username;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(name = "activity_date", nullable = false)
    private LocalDateTime activityDate;
    
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
    
    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
    
    @Column(name = "ip", length = 50)
    private String ip;
    
    @Column(name = "session_id", length = 255)
    private String sessionId;
    
    @PrePersist
    protected void onCreate() {
        createdDate = LocalDateTime.now();
        updatedDate = LocalDateTime.now();
        if (activityDate == null) {
            activityDate = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedDate = LocalDateTime.now();
    }
}
