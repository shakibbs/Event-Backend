package com.event_management_system.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public abstract class BaseEntity {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "status", length = 20)
    private Status status = Status.ACTIVE;

    @Column(nullable = false, name = "deleted")
    private Boolean deleted = false;

    public enum Status {
        ACTIVE,     // visible / usable
        INACTIVE,   // temporarily hidden
        OFF         // permanently closed / soft-deleted
    }


    /**
     * Record creation info
     */
    public void recordCreation(String user) {
        this.createdBy = user;
        this.status = Status.ACTIVE;
        this.deleted = false;
    }

    /**
     * Record update info
     */
    public void recordUpdate(String user) {
        this.updatedBy = user;
    }

    /**
     * Soft-delete the entity
     */
    public void markDeleted() {
        this.status = Status.OFF;
        this.deleted = true;
    }

    /**
     * Temporarily hide the entity
     */
    public void deactivate() {
        if (!Boolean.TRUE.equals(this.deleted)) {
            this.status = Status.INACTIVE;
        }
    }



        public void activate() {
        this.status = Status.ACTIVE;
        this.deleted = false;
    }


    public boolean isActive() {
        return this.status == Status.ACTIVE && !Boolean.TRUE.equals(this.deleted);
    }

    public boolean isInactive() {
        return this.status == Status.INACTIVE && !Boolean.TRUE.equals(this.deleted);
    }

    public boolean isOff() {
        return this.status == Status.OFF && Boolean.TRUE.equals(this.deleted);
    }

    public boolean isDeleted() {
        return Boolean.TRUE.equals(this.deleted);
    }

       public static boolean isDateRangeValid(LocalDateTime start, LocalDateTime end) {
    return start == null || end == null || end.isAfter(start);
    }

    // ====== Convenience copy method ======
    public void copyAuditFrom(BaseEntity other) {
        if (other == null) return;
        this.createdAt = other.createdAt;
        this.createdBy = other.createdBy;
        this.updatedAt = other.updatedAt;
        this.updatedBy = other.updatedBy;
        this.status = other.status;
        this.deleted = other.deleted;
    }
}
