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
@Table(name = "user_login_logout_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginLogoutHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "user_token", length = 500, nullable = false)
    private String userToken;
    
    @Column(name = "user_type", length = 100, nullable = false)
    private String userType;
    
    @Column(name = "request_from", length = 100)
    private String requestFrom;
    
    @Column(name = "request_ip", length = 50, nullable = false)
    private String requestIp;
    
    @Column(name = "device_info", length = 500)
    private String deviceInfo;
    
    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;
    
    @Column(name = "logout_time")
    private LocalDateTime logoutTime;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "login_status", length = 255)
    private String loginStatus;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
