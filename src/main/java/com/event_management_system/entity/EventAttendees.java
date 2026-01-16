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
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "event_attendees")
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAttendees extends BaseEntity {

    public enum InvitationStatus {
        PENDING("Pending"),
        ACCEPTED("Accepted"),
        DECLINED("Declined");

        private final String displayName;

        InvitationStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @ManyToOne
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Email(message = "Invalid email format")
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "invitation_status", nullable = false, length = 20)
    @Builder.Default
    private InvitationStatus invitationStatus = InvitationStatus.PENDING;

    @Column(name = "invitation_token", unique = true, nullable = false, length = 36)
    private String invitationToken;

    @Column(name = "invitation_sent_at")
    private LocalDateTime invitationSentAt;

    @Column(name = "response_at")
    private LocalDateTime responseAt;

    @Column(name = "advance_reminder_sent", nullable = false)
    @Builder.Default
    private Boolean advanceReminderSent = false;

    @Column(name = "last_minute_reminder_sent", nullable = false)
    @Builder.Default
    private Boolean lastMinuteReminderSent = false;

    @PrePersist
    public void generateInvitationToken() {
        if (this.invitationToken == null) {
            this.invitationToken = java.util.UUID.randomUUID().toString();
        }
    }
}