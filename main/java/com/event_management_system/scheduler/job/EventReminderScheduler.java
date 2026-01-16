package com.event_management_system.scheduler.job;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.entity.Event;
import com.event_management_system.entity.EventAttendees;
import com.event_management_system.repository.EventAttendeesRepository;
import com.event_management_system.service.ApplicationLoggerService;
import com.event_management_system.service.EmailService;

@Component
public class EventReminderScheduler {

    @Autowired
    private EventAttendeesRepository eventAttendeesRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationLoggerService log;

    @Scheduled(fixedDelay = 300000, initialDelay = 10000) // Every 5 minutes
    @Transactional
    public void sendEventReminders() {
        try {
            log.info("[EventReminderScheduler] INFO - sendEventReminders() - Scheduler job started");
            
            LocalDateTime now = LocalDateTime.now();
            
            // Send 24-hour advance reminders
            send24HourReminders(now);
            
            // Send 2-hour last-minute reminders
            send2HourReminders(now);
            
            log.info("[EventReminderScheduler] INFO - sendEventReminders() - Job completed");
            
        } catch (Exception e) {
            log.error("[EventReminderScheduler] ERROR - sendEventReminders() - Critical error in scheduler job: " + e.getMessage());
        }
    }

    private void send24HourReminders(LocalDateTime now) {
        log.info("[EventReminderScheduler] INFO - send24HourReminders() - Checking for events starting in 24 hours");
        
        LocalDateTime twentyFourHours = now.plusHours(24);
        
        List<EventAttendees> attendeesNeedingReminder = eventAttendeesRepository.findAll()
                .stream()
                .filter(ea -> !ea.getDeleted())
                .filter(ea -> ea.getEvent() != null && !ea.getEvent().getDeleted())
                .filter(ea -> ea.getEvent().getEventStatus() == Event.EventStatus.UPCOMING)
                .filter(ea -> ea.getEvent().getApprovalStatus() == Event.ApprovalStatus.APPROVED)
                .filter(ea -> ea.getInvitationStatus() == EventAttendees.InvitationStatus.ACCEPTED)
                .filter(ea -> !ea.getAdvanceReminderSent())
                .filter(ea -> ea.getEvent().getStartTime().isAfter(now))
                .filter(ea -> ea.getEvent().getStartTime().isBefore(twentyFourHours))
                .toList();
        
        log.info("[EventReminderScheduler] INFO - send24HourReminders() - Found " + attendeesNeedingReminder.size() + " attendees needing 24-hour reminder");
        
        int sent = 0, failed = 0;
        
        for (EventAttendees attendee : attendeesNeedingReminder) {
            if (sendReminderEmail(attendee, "24-hour")) {
                attendee.setAdvanceReminderSent(true);
                attendee.recordUpdate("system");
                eventAttendeesRepository.save(attendee);
                sent++;
            } else {
                failed++;
            }
        }
        
        log.info("[EventReminderScheduler] INFO - send24HourReminders() - Sent: " + sent + ", Failed: " + failed);
    }

    private void send2HourReminders(LocalDateTime now) {
        log.info("[EventReminderScheduler] INFO - send2HourReminders() - Checking for events starting in 2 hours");
        
        LocalDateTime twoHours = now.plusHours(2);
        LocalDateTime twoHoursThirty = now.plusMinutes(150); // 2.5 hours
        
        List<EventAttendees> attendeesNeedingReminder = eventAttendeesRepository.findAll()
                .stream()
                .filter(ea -> !ea.getDeleted())
                .filter(ea -> ea.getEvent() != null && !ea.getEvent().getDeleted())
                .filter(ea -> ea.getEvent().getEventStatus() == Event.EventStatus.UPCOMING)
                .filter(ea -> ea.getEvent().getApprovalStatus() == Event.ApprovalStatus.APPROVED)
                .filter(ea -> ea.getInvitationStatus() == EventAttendees.InvitationStatus.ACCEPTED)
                .filter(ea -> !ea.getLastMinuteReminderSent())
                .filter(ea -> ea.getEvent().getStartTime().isAfter(twoHours))
                .filter(ea -> ea.getEvent().getStartTime().isBefore(twoHoursThirty))
                .toList();
        
        log.info("[EventReminderScheduler] INFO - send2HourReminders() - Found " + attendeesNeedingReminder.size() + " attendees needing 2-hour reminder");
        
        int sent = 0, failed = 0;
        
        for (EventAttendees attendee : attendeesNeedingReminder) {
            if (sendReminderEmail(attendee, "2-hour")) {
                attendee.setLastMinuteReminderSent(true);
                attendee.recordUpdate("system");
                eventAttendeesRepository.save(attendee);
                sent++;
            } else {
                failed++;
            }
        }
        
        log.info("[EventReminderScheduler] INFO - send2HourReminders() - Sent: " + sent + ", Failed: " + failed);
    }

    private boolean sendReminderEmail(EventAttendees attendee, String reminderType) {
        try {
            Event event = attendee.getEvent();
            
            if (attendee.getEmail() == null || attendee.getEmail().trim().isEmpty()) {
                log.warn("[EventReminderScheduler] WARN - sendReminderEmail() - No email for attendee, event: " + event.getTitle());
                return false;
            }
            
            if (attendee.getUser() == null) {
                log.warn("[EventReminderScheduler] WARN - sendReminderEmail() - External email '" + attendee.getEmail() + "' has no user account. Skipping.");
                return false;
            }
            
            log.debug("[EventReminderScheduler] DEBUG - sendReminderEmail() - Sending " + reminderType + " reminder to '" + attendee.getEmail() + "' for event '" + event.getTitle() + "'");
            
            boolean emailSent = emailService.sendEventReminder(event, attendee.getUser());
            
            if (emailSent) {
                log.info("[EventReminderScheduler] INFO - sendReminderEmail() - " + reminderType + " reminder sent to '" + attendee.getEmail() + "' for event '" + event.getTitle() + "'");
            } else {
                log.error("[EventReminderScheduler] ERROR - sendReminderEmail() - Failed to send " + reminderType + " reminder to '" + attendee.getEmail() + "'");
            }
            
            return emailSent;
            
        } catch (Exception e) {
            log.error("[EventReminderScheduler] ERROR - sendReminderEmail() - Error: " + e.getMessage());
            return false;
        }
    }
}
