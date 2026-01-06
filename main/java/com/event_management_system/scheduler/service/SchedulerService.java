package com.event_management_system.scheduler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.event_management_system.scheduler.job.AuditLogArchivalJob;
import com.event_management_system.scheduler.job.EventReminderJob;
import com.event_management_system.scheduler.job.EventStatusUpdateJob;
import com.event_management_system.service.ApplicationLoggerService;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class SchedulerService {

    @Autowired
    private EventReminderJob eventReminderJob;

    @Autowired
    private EventStatusUpdateJob eventStatusUpdateJob;

    @Autowired
    private AuditLogArchivalJob auditLogArchivalJob;

    @Autowired
    private ApplicationLoggerService logger;

 
    public void triggerEventReminder() {
        try {
            logger.infoWithContext("SchedulerService", "Manually triggering Event Reminder Job");
            eventReminderJob.sendEventReminders();
            logger.infoWithContext("SchedulerService", "Event Reminder Job completed successfully");
        } catch (Exception e) {
            logger.errorWithContext("SchedulerService", "Error triggering Event Reminder Job", e);
            throw new RuntimeException("Failed to trigger event reminder job", e);
        }
    }

    
    
    public void triggerEventStatusUpdate() {
        try {
            logger.infoWithContext("SchedulerService", "Manually triggering Event Status Update Job");
            eventStatusUpdateJob.updateEventStatus();
            logger.infoWithContext("SchedulerService", "Event Status Update Job completed successfully");
        } catch (Exception e) {
            logger.errorWithContext("SchedulerService", "Error triggering Event Status Update Job", e);
            throw new RuntimeException("Failed to trigger event status update job", e);
        }
    }

    
    public void triggerAuditLogArchival() {
        try {
            logger.infoWithContext("SchedulerService", "Manually triggering Audit Log Archival Job");
            auditLogArchivalJob.archiveOldLogs();
            logger.infoWithContext("SchedulerService", "Audit Log Archival Job completed successfully");
        } catch (Exception e) {
            logger.errorWithContext("SchedulerService", "Error triggering Audit Log Archival Job", e);
            throw new RuntimeException("Failed to trigger audit log archival job", e);
        }
    }

    
    public String getSchedulerStatus() {
        logger.debugWithContext("SchedulerService", "Checking scheduler status");
        return "Scheduler is running. All jobs are registered and waiting for their scheduled times.";
    }
}
