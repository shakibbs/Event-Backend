package com.event_management_system.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.UserLoginLogoutHistory;

/**
 * UserLoginLogoutHistoryRepository
 * 
 * Manages database operations for UserLoginLogoutHistory entity
 * Spring Data JPA automatically handles all queries!
 * 
 * Purpose:
 * - Track user login/logout events
 * - Find active sessions (users currently logged in)
 * - Analyze login patterns and security
 * - Detect suspicious login activities
 */
@Repository
public interface UserLoginLogoutHistoryRepository extends JpaRepository<UserLoginLogoutHistory, Long> {
    
    /**
     * Find all login/logout records for a specific user
     */
    List<UserLoginLogoutHistory> findByUserId(Long userId);
    
    /**
     * Find login record by JWT token
     */
    Optional<UserLoginLogoutHistory> findByUserToken(String token);
    
    /**
     * Find ACTIVE sessions for a user (logout_time IS NULL)
     */
    List<UserLoginLogoutHistory> findByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(Long userId);
    
    /**
     * Find login records for a user within a date range
     */
    List<UserLoginLogoutHistory> findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
    /**
     * Find failed login attempts for a user
     */
    List<UserLoginLogoutHistory> findByUserIdAndLoginStatusOrderByLoginTimeDesc(Long userId, String status);
    
    /**
     * Count active sessions for a user
     */
    long countByUserIdAndLogoutTimeIsNull(Long userId);
    
    /**
     * Find the most recent login for a user
     */
    Optional<UserLoginLogoutHistory> findFirstByUserIdOrderByLoginTimeDesc(Long userId);
}
