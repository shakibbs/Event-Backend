package com.event_management_system.scheduler.job;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.service.ApplicationLoggerService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AuditLogArchivalJob {

    @Autowired
    private ApplicationLoggerService logger;

    
    @Scheduled(fixedDelay = 86400000, initialDelay = 60000)  // 24 hours, wait 1 min first
    @Transactional  
    public void archiveOldLogs() {
        try {
            logger.infoWithContext("AuditLogArchivalJob", "Scheduler job started - Archiving old audit logs");
            
            // Calculate cutoff date: 90 days ago
            LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
            logger.debugWithContext("AuditLogArchivalJob", "Deleting logs older than: {}", ninetyDaysAgo);
            
            int activityHistoryDeleted = 0;
            int loginHistoryDeleted = 0;
            int passwordHistoryDeleted = 0;
            

            logger.infoWithContext("AuditLogArchivalJob", 
                    "Log archival completed. Deleted activity records: {}, Login records: {}, Password records: {}",
                    activityHistoryDeleted, loginHistoryDeleted, passwordHistoryDeleted);
            
            logger.infoWithContext("AuditLogArchivalJob", 
                    "Total audit logs archived: {}",
                    (activityHistoryDeleted + loginHistoryDeleted + passwordHistoryDeleted));
            
        } catch (Exception e) {
            logger.errorWithContext("AuditLogArchivalJob", "Unexpected error in archiveOldLogs()", e);
        }
    }
}
