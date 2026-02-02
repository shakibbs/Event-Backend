package com.event_management_system.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.UserActivityHistory;


@Repository
public interface UserActivityHistoryRepository extends JpaRepository<UserActivityHistory, Long> {
    
   
    List<UserActivityHistory> findByUserId(Long userId);
    
 
    @Query("SELECT uah FROM UserActivityHistory uah " +
           "LEFT JOIN FETCH uah.createdBy " +
           "LEFT JOIN FETCH uah.updatedBy " +
           "WHERE uah.activityDate BETWEEN :startDate AND :endDate " +
           "ORDER BY uah.activityDate DESC")
    List<UserActivityHistory> findAllWithCreatedByAndUpdatedBy(@Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);
    
 
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
