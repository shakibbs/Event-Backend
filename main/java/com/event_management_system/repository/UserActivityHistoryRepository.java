package com.event_management_system.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.UserActivityHistory;

/**
 * UserActivityHistoryRepository
 * 
 * Manages database operations for UserActivityHistory entity
 * Spring Data JPA automatically handles all queries!
 * 
 * Purpose:
 * - Track all user activities in the system
 * - Create complete audit trails
 * - Group activities by session
 * - Filter by activity type and user role
 */
@Repository
public interface UserActivityHistoryRepository extends JpaRepository<UserActivityHistory, Long> {
    
    /**
     * Find all activities for a user
     */
    List<UserActivityHistory> findByUserId(Long userId);
    
    /**
     * Find activities within a date range
     */
    List<UserActivityHistory> findByActivityDateBetweenOrderByActivityDateDesc(
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
    /**
     * Find all activities of a specific type
     */
    List<UserActivityHistory> findByActivityTypeCodeOrderByActivityDateDesc(String activityTypeCode);
    
    /**
     * Find all activities for a user within a date range
     */
    List<UserActivityHistory> findByUserIdAndActivityDateBetweenOrderByActivityDateDesc(
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
    /**
     * Find all activities in a session (groups related activities)
     */
    List<UserActivityHistory> findBySessionIdOrderByActivityDateAsc(String sessionId);
    
    /**
     * Find activities by user role/group
     */
    List<UserActivityHistory> findByUserGroupOrderByActivityDateDesc(String userGroup);
    
    /**
     * Count total activities for a user
     */
    long countByUserId(Long userId);
}
