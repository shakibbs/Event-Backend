package com.event_management_system.scheduler.job;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.entity.Event;
import com.event_management_system.entity.EventReminderSent;
import com.event_management_system.entity.User;
import com.event_management_system.repository.EventReminderSentRepository;
import com.event_management_system.repository.EventRepository;
import com.event_management_system.service.ApplicationLoggerService;
import com.event_management_system.service.EmailService;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class EventReminderJob {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventReminderSentRepository eventReminderSentRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ApplicationLoggerService logger;

    @Scheduled(fixedDelay = 300000, initialDelay = 10000)  // Runs every 5 minutes (300,000ms), starts 10s after app launch
    @Transactional(readOnly = true)  // Read-only transaction for better performance
    public void sendEventReminders() {
        try {
            // Log start of job execution
            logger.infoWithContext(
                "EventReminderJob", 
                "sendEventReminders",
                "Scheduler job started - Checking for events starting in 2 hours"
            );
            
            // ========== STEP 1: Calculate Time Window ==========
            // We want events starting in approximately 2 hours
            // Since job runs every 5 minutes, we use a window: 1.5 to 2.5 hours
            // This ensures we catch each event exactly once
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime twoHoursLater = now.plusHours(2);           // 2 hours from now
            LocalDateTime twoHoursThirtyMinutes = now.plusMinutes(150); // 2.5 hours (upper bound)
            
           
            
            logger.debugWithContext(
                "EventReminderJob",
                "sendEventReminders", 
                "Searching for events between {} and {}",
                twoHoursLater, twoHoursThirtyMinutes
            );
            
        
            List<Event> upcomingEvents = eventRepository.findAll()
                    .stream()
                    .filter(event -> !event.getDeleted())  // Not soft-deleted
                    .filter(event -> event.getStatus() == Event.Status.ACTIVE)  // Active events only
                    .filter(event -> event.getStartTime().isAfter(twoHoursLater))  // Starts after 2 hours
                    .filter(event -> event.getStartTime().isBefore(twoHoursThirtyMinutes))  // Starts before 2.5 hours
                    .toList();
            
            logger.infoWithContext(
                "EventReminderJob",
                "sendEventReminders", 
                "Found {} upcoming events in 2-hour window",
                upcomingEvents.size()
            );
            
          
            
            int totalEventsProcessed = 0;
            int totalEmailsSent = 0;
            int totalEmailsSkipped = 0;
            int totalEmailsFailed = 0;
            
            for (Event event : upcomingEvents) {
                try {
                    logger.debugWithContext(
                        "EventReminderJob",
                        "sendEventReminders",
                        "Processing event: '{}' (ID: {}), Starting at: {}",
                        event.getTitle(), event.getId(), event.getStartTime()
                    );
                    
                  
                    
                    Set<User> attendees = event.getAttendees();
                    
                    if (attendees == null || attendees.isEmpty()) {
                        logger.debugWithContext(
                            "EventReminderJob",
                            "sendEventReminders",
                            "No attendees for event: '{}' (ID: {}). Skipping.",
                            event.getTitle(), event.getId()
                        );
                        totalEventsProcessed++;
                        continue;  // Skip to next event
                    }
                    
                    logger.infoWithContext(
                        "EventReminderJob",
                        "sendEventReminders",
                        "Processing {} attendees for event: '{}'",
                        attendees.size(), event.getTitle()
                    );
                    
                    
                    int eventEmailsSent = 0;
                    int eventEmailsSkipped = 0;
                    int eventEmailsFailed = 0;
                    
                    for (User attendee : attendees) {
                        try {
                            
                            
                            boolean alreadySent = eventReminderSentRepository
                                .existsByEventIdAndUserId(event.getId(), attendee.getId());
                            
                            if (alreadySent) {
                                logger.debugWithContext(
                                    "EventReminderJob",
                                    "sendEventReminders",
                                    "Reminder already sent to user '{}' (ID: {}) for event '{}'. Skipping.",
                                    attendee.getFullName(), attendee.getId(), event.getTitle()
                                );
                                eventEmailsSkipped++;
                                continue;  // Skip to next attendee
                            }
                            
                          
                            
                            if (attendee.getEmail() == null || attendee.getEmail().trim().isEmpty()) {
                                logger.warnWithContext(
                                    "EventReminderJob",
                                    "sendEventReminders",
                                    "User '{}' (ID: {}) has no email address. Cannot send reminder.",
                                    attendee.getFullName(), attendee.getId()
                                );
                                eventEmailsFailed++;
                                continue;  // Skip to next attendee
                            }
                            
                            
                            logger.debugWithContext(
                                "EventReminderJob",
                                "sendEventReminders",
                                "Sending reminder to '{}' ({}) for event '{}'",
                                attendee.getFullName(), attendee.getEmail(), event.getTitle()
                            );
                            
                            boolean emailSent = emailService.sendEventReminder(event, attendee);
                            
                            if (emailSent) {
                               
                                
                                EventReminderSent reminderRecord = EventReminderSent.builder()
                                    .event(event)
                                    .user(attendee)
                                    .emailSentTo(attendee.getEmail())
                                    .sentAt(LocalDateTime.now())
                                    .deliveryStatus("SENT")
                                    .build();
                                
                                eventReminderSentRepository.save(reminderRecord);
                                
                                logger.infoWithContext(
                                    "EventReminderJob",
                                    "sendEventReminders",
                                    "Successfully sent reminder to '{}' for event '{}'",
                                    attendee.getEmail(), event.getTitle(),
                                    "eventId", event.getId(),
                                    "userId", attendee.getId()
                                );
                                
                                eventEmailsSent++;
                                
                            } else {
                                // Email sending failed (SMTP error, network issue, etc.)
                                logger.warnWithContext(
                                    "EventReminderJob",
                                    "sendEventReminders",
                                    "Failed to send reminder to '{}' for event '{}' (eventId={}, userId={})",
                                    attendee.getEmail(), event.getTitle(), event.getId(), attendee.getId()
                                );
                                eventEmailsFailed++;
                            }
                            
                        } catch (Exception e) {
                            // Catch errors for individual attendee processing
                            // Log but continue with next attendee
                            logger.errorWithContext(
                                "EventReminderJob",
                                "sendEventReminders - Error processing attendee " + attendee.getId(),
                                e
                            );
                            eventEmailsFailed++;
                        }
                    }
                    
                    // ========== STEP 6: Log Event Summary ==========
                    logger.infoWithContext(
                        "EventReminderJob",
                        "sendEventReminders",
                        "Event '{}' processed. Sent: {}, Skipped: {}, Failed: {}",
                        event.getTitle(), eventEmailsSent, eventEmailsSkipped, eventEmailsFailed
                    );
                    
                    totalEventsProcessed++;
                    totalEmailsSent += eventEmailsSent;
                    totalEmailsSkipped += eventEmailsSkipped;
                    totalEmailsFailed += eventEmailsFailed;
                    
                } catch (Exception e) {
                    // Log error for individual event but continue with next event
                    logger.errorWithContext(
                        "EventReminderJob",
                        "sendEventReminders - Error processing event " + event.getId(),
                        e
                    );
                }
            }
            
            // ========== STEP 7: Log Job Summary ==========
            logger.infoWithContext(
                "EventReminderJob",
                "sendEventReminders",
                "Job completed. Events: {}, Emails sent: {}, Skipped: {}, Failed: {}",
                totalEventsProcessed, totalEmailsSent, totalEmailsSkipped, totalEmailsFailed
            );
            
        } catch (Exception e) {
            // Catch any unexpected errors at job level
            logger.errorWithContext(
                "EventReminderJob",
                "sendEventReminders - Unexpected error in job",
                e
            );
            // Don't throw - let the scheduler continue running
        }
    }
}
