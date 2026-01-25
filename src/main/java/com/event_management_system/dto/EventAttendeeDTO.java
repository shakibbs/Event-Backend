package com.event_management_system.dto;

import java.time.LocalDateTime;

import com.event_management_system.entity.EventAttendees.InvitationStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventAttendeeDTO {
    private Long id;
    private String email;
    private InvitationStatus invitationStatus;
    private String invitationToken;
    private LocalDateTime invitationSentAt;
    private LocalDateTime responseAt;
    private Boolean advanceReminderSent;
    private Boolean lastMinuteReminderSent;
    private Long userId;
    private String userFullName;
}
