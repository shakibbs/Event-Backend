package com.event_management_system.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.UserPasswordHistory;

/**
 * UserPasswordHistoryRepository
 * 
 * Manages database operations for UserPasswordHistory entity
 * Spring Data JPA automatically handles all queries!
 * 
 * Purpose:
 * - Track password change history
 * - Find when passwords were changed
 * - Track who changed passwords (user or admin)
 * - Security audit trail for password modifications
 */
@Repository
public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, Long> {
    
    /**
     * Find all password changes for a user
     */
    List<UserPasswordHistory> findByUserId(Long userId);
    
    /**
     * Find the most recent password change for a user
     */
    Optional<UserPasswordHistory> findFirstByUserIdOrderByChangeDateDesc(Long userId);
    
    /**
     * Find password changes within a date range
     */
    List<UserPasswordHistory> findByUserIdAndChangeDateBetweenOrderByChangeDateDesc(
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
    /**
     * Count total password changes for a user
     */
    long countByUserId(Long userId);
}
