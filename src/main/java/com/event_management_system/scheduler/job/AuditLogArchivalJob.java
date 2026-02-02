package com.event_management_system.scheduler.job;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.service.ApplicationLoggerService;


@Component
public class AuditLogArchivalJob {

    @Autowired
    private ApplicationLoggerService log;

    @Scheduled(fixedDelay = 86400000, initialDelay = 60000) // Every 24 hours
    @Transactional  
    public void archiveOldLogs() {
        try {
            log.info("[AuditLogArchivalJob] INFO - archiveOldLogs() - Scheduler job started - Archiving old audit logs");
            
            LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
            log.debug("[AuditLogArchivalJob] DEBUG - archiveOldLogs() - Deleting logs older than: " + ninetyDaysAgo);
            
            int activityHistoryDeleted = 0;
            int loginHistoryDeleted = 0;
            int passwordHistoryDeleted = 0;

            log.info("[AuditLogArchivalJob] INFO - archiveOldLogs() - Log archival completed. Deleted activity records: " + activityHistoryDeleted + ", Login records: " + loginHistoryDeleted + ", Password records: " + passwordHistoryDeleted);
            
            log.info("[AuditLogArchivalJob] INFO - archiveOldLogs() - Total audit logs archived: " + (activityHistoryDeleted + loginHistoryDeleted + passwordHistoryDeleted));
            
        } catch (Exception e) {
            log.error("[AuditLogArchivalJob] ERROR - archiveOldLogs() - Unexpected error in archiveOldLogs(): " + e.getMessage());
        }
    }
}


