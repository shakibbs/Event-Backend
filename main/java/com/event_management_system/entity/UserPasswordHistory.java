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
 * UserPasswordHistory Entity
 * 
 * Records every password change for audit trail and security
 * 
 * Fields:
 * - user_id: Which user's password was changed
 * - password_change_by: Who changed it (user themselves or admin)
 * - change_date: When was it changed
 * - old_password: Hash of old password (for audit)
 * - new_password: Hash of new password (for audit)
 * - created_by: System user who created this record
 * - created_at: When this record was created
 * - updated_at: When this record was last updated
 * - is_active: Is this record active
 * 
 * Flow:
 * User changes password → Record created → Stored in this table
 * Later: Can retrieve "when did user last change password?"
 * Can check: "User changed password 5 times in last month"
 */
@Entity
@Table(name = "user_password_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPasswordHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The user whose password was changed
     * Foreign key to app_users table
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * The user who changed the password
     * Could be the user themselves or an admin
     * Foreign key to app_users table
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "password_change_by", nullable = false)
    private User passwordChangedBy;
    
    /**
     * When the password was changed
     */
    @Column(name = "change_date", nullable = false)
    private LocalDateTime changeDate;
    
    /**
     * Hash of the old password (before change)
     * Stored for audit trail
     */
    @Column(name = "old_password", length = 255)
    private String oldPassword;
    
    /**
     * Hash of the new password (after change)
     * Stored for audit trail
     */
    @Column(name = "new_password", length = 255)
    private String newPassword;
    
    /**
     * Which user/system created this record
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
     * Auto-set created_at timestamp when record is created
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Auto-set updated_at timestamp when record is updated
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
