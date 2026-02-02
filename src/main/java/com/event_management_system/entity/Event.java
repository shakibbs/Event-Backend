package com.event_management_system.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event extends BaseEntity {

    public enum Visibility {
        PUBLIC("Public"),
        PRIVATE("Private");

        private final String displayName;

        Visibility(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ApprovalStatus {
        PENDING("Pending"),
        APPROVED("Approved"),
        REJECTED("Rejected");

        private final String displayName;

        ApprovalStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum EventStatus {
        UPCOMING,
        INACTIVE,
        ONGOING,
        COMPLETED,
        CANCELLED;
    }

    @NotBlank(message = "Event title is required")
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Start time is required")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotBlank(message = "Location is required")
    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility = Visibility.PUBLIC;

    @ManyToOne
    @JoinColumn(name = "organizer_id", nullable = false)
    private User organizer;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_status", nullable = false, length = 20)
    private EventStatus eventStatus = EventStatus.UPCOMING;

    @PrePersist
    protected void onCreate() {
        if (approvalStatus == null) {
            approvalStatus = ApprovalStatus.PENDING;
        }
    }

    public EventStatus getCurrentEventStatus() {
        if (this.eventStatus == EventStatus.INACTIVE || this.eventStatus == EventStatus.CANCELLED) {
            return this.eventStatus;
        }

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(this.startTime)) {
            return EventStatus.UPCOMING;
        } else if (now.isAfter(this.endTime)) {
            return EventStatus.COMPLETED;
        } else {
            return EventStatus.ONGOING;
        }
    }

    public void hold() {
        this.eventStatus = EventStatus.INACTIVE;
    }

    public void cancel() {
        this.eventStatus = EventStatus.CANCELLED;
    }

    public void reactivate() {
        this.eventStatus = EventStatus.UPCOMING;
    }

    public boolean isHoldable() {
        return this.eventStatus == EventStatus.UPCOMING;
    }

    public boolean isReactivatable() {
        return this.eventStatus == EventStatus.INACTIVE;
    }

    public String getName() {
        return this.title;
    }

}
