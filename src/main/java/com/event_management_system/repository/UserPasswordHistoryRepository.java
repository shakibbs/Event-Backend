package com.event_management_system.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.UserPasswordHistory;


@Repository
public interface UserPasswordHistoryRepository extends JpaRepository<UserPasswordHistory, Long> {
    
    
    List<UserPasswordHistory> findByUserId(Long userId);
    
 
    Optional<UserPasswordHistory> findFirstByUserIdOrderByChangeDateDesc(Long userId);
    
    
    List<UserPasswordHistory> findByUserIdAndChangeDateBetweenOrderByChangeDateDesc(
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
  
    long countByUserId(Long userId);
}
