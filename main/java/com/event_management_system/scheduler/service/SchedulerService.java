package com.event_management_system.scheduler.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.event_management_system.scheduler.job.AuditLogArchivalJob;
import com.event_management_system.scheduler.job.EventReminderScheduler;
import com.event_management_system.service.ApplicationLoggerService;


@Service
public class SchedulerService {

    @Autowired
    private EventReminderScheduler eventReminderScheduler;

    @Autowired
    private AuditLogArchivalJob auditLogArchivalJob;

    @Autowired
    private ApplicationLoggerService log;

    public void triggerEventReminder() {
        try {
            log.info("[SchedulerService] INFO - triggerEventReminder() - Manually triggering Event Reminder Job");
            eventReminderScheduler.sendEventReminders();
            log.info("[SchedulerService] INFO - triggerEventReminder() - Event Reminder Job completed successfully");
        } catch (Exception e) {
            log.error("[SchedulerService] ERROR - triggerEventReminder() - Error triggering Event Reminder Job: " + e.getMessage());
            throw new RuntimeException("Failed to trigger event reminder job", e);
        }
    }

    public void triggerAuditLogArchival() {
        try {
            log.info("[SchedulerService] INFO - triggerAuditLogArchival() - Manually triggering Audit Log Archival Job");
            auditLogArchivalJob.archiveOldLogs();
            log.info("[SchedulerService] INFO - triggerAuditLogArchival() - Audit Log Archival Job completed successfully");
        } catch (Exception e) {
            log.error("[SchedulerService] ERROR - triggerAuditLogArchival() - Error triggering Audit Log Archival Job: " + e.getMessage());
            throw new RuntimeException("Failed to trigger audit log archival job", e);
        }
    }

    public String getSchedulerStatus() {
        log.debug("[SchedulerService] DEBUG - getSchedulerStatus() - Checking scheduler status");
        return "Scheduler is running. All jobs are registered and waiting for their scheduled times.";
    }
}


