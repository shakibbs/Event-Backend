package com.event_management_system.scheduler.job;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.entity.Event;
import com.event_management_system.repository.EventRepository;
import com.event_management_system.service.ApplicationLoggerService;

import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class EventStatusUpdateJob {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ApplicationLoggerService logger;

    
    
    @Scheduled(fixedDelay = 1800000, initialDelay = 20000)  // 30 minutes, wait 20 sec first
    @Transactional  // Can write to database, so @Transactional (not readOnly)
    public void updateEventStatus() {
        try {
            logger.infoWithContext("EventStatusUpdateJob", "Scheduler job started - Updating event statuses");
            
            LocalDateTime now = LocalDateTime.now();
            logger.debugWithContext("EventStatusUpdateJob", "Current time: {}", now);
            
            // Get all ACTIVE events from database
            List<Event> allEvents = eventRepository.findAll()
                    .stream()
                    .filter(event -> !event.isDeleted())  // Not soft-deleted
                    .filter(event -> "ACTIVE".equals(event.getStatus().toString()))  // Status is ACTIVE
                    .toList();
            
            logger.infoWithContext("EventStatusUpdateJob", "Found {} active events to check", allEvents.size());
            
            int completedCount = 0;
            
            // Check each event
            for (Event event : allEvents) {
                try {
                    // If event end time has passed, mark as COMPLETED
                    if (event.getEndTime().isBefore(now)) {
                        logger.debugWithContext("EventStatusUpdateJob", 
                                "Event '{}' (ID: {}) has ended. Updating status to COMPLETED", 
                                event.getTitle(), event.getId());
                        
                        eventRepository.save(event);
                        
                        logger.infoWithContext("EventStatusUpdateJob", 
                                "Event '{}' (ID: {}) marked as COMPLETED", 
                                event.getTitle(), event.getId());
                        
                        completedCount++;
                    }
                    // If event start time has passed but end time hasn't, mark as STARTED (optional)
                    else if (event.getStartTime().isBefore(now)) {
                        logger.debugWithContext("EventStatusUpdateJob", 
                                "Event '{}' (ID: {}) has started", 
                                event.getTitle(), event.getId());
                        
                        
                    }
                    
                } catch (Exception e) {
                    logger.errorWithContext("EventStatusUpdateJob", 
                            "Error updating status for event: " + event.getId(), e);
                }
            }
            
            logger.infoWithContext("EventStatusUpdateJob", 
                    "Event status update job completed. Updated {} events to COMPLETED", completedCount);
            
        } catch (Exception e) {
            logger.errorWithContext("EventStatusUpdateJob", "Unexpected error in updateEventStatus()", e);
        }
    }
}
