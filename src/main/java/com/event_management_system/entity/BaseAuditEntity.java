package com.event_management_system.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(nullable = false, name = "deleted")
    private Boolean deleted = false;

    public void recordCreation(String user) {
        this.createdBy = user;
        this.deleted = false;
    }

    public void recordUpdate(String user) {
        this.updatedBy = user;
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.deleted);
    }

    public void copyAuditFrom(BaseAuditEntity other) {
        if (other == null) return;
        this.createdAt = other.createdAt;
        this.createdBy = other.createdBy;
        this.updatedAt = other.updatedAt;
        this.updatedBy = other.updatedBy;
        this.deleted = other.deleted;
    }
}
