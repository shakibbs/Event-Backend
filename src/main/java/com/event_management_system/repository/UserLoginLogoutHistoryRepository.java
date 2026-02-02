package com.event_management_system.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.UserLoginLogoutHistory;


@Repository
public interface UserLoginLogoutHistoryRepository extends JpaRepository<UserLoginLogoutHistory, Long> {
    
  
    List<UserLoginLogoutHistory> findByUserId(Long userId);
 
    Optional<UserLoginLogoutHistory> findByUserToken(String token);
    
    /**
     * Find ACTIVE sessions for a user (logout_time IS NULL)
     */
    List<UserLoginLogoutHistory> findByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(Long userId);
  
    List<UserLoginLogoutHistory> findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
  
    List<UserLoginLogoutHistory> findByUserIdAndLoginStatusOrderByLoginTimeDesc(Long userId, String status);
    
 
    long countByUserIdAndLogoutTimeIsNull(Long userId);
    
   
    Optional<UserLoginLogoutHistory> findFirstByUserIdOrderByLoginTimeDesc(Long userId);
}
