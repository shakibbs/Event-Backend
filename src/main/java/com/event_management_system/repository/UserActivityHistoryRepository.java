package com.event_management_system.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.UserActivityHistory;


@Repository
public interface UserActivityHistoryRepository extends JpaRepository<UserActivityHistory, Long> {
    
   
    List<UserActivityHistory> findByUserId(Long userId);
    
 
    List<UserActivityHistory> findByActivityDateBetweenOrderByActivityDateDesc(
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
 
    List<UserActivityHistory> findByActivityTypeCodeOrderByActivityDateDesc(String activityTypeCode);
   
    List<UserActivityHistory> findByUserIdAndActivityDateBetweenOrderByActivityDateDesc(
        Long userId,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
  
    List<UserActivityHistory> findBySessionIdOrderByActivityDateAsc(String sessionId);
    
  
    List<UserActivityHistory> findByUserGroupOrderByActivityDateDesc(String userGroup);
    
   
     
    long countByUserId(Long userId);
}
