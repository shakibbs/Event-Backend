
package com.event_management_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.dto.UserActivityHistoryResponseDTO;
import com.event_management_system.entity.User;
import com.event_management_system.entity.UserActivityHistory;
import com.event_management_system.entity.UserActivityHistory.ActivityType;
import com.event_management_system.mapper.UserActivityHistoryMapper;
import com.event_management_system.repository.UserActivityHistoryRepository;


@Service
public class UserActivityHistoryService {

        @Transactional
        public void deleteAllByUserId(Long userId) {
                activityHistoryRepository.deleteAll(activityHistoryRepository.findByUserId(userId));
        }
    
    @Autowired
    private UserActivityHistoryRepository activityHistoryRepository;
    
    @Autowired
    private UserActivityHistoryMapper activityHistoryMapper;
    
   
    @Transactional
    public UserActivityHistory recordActivity(
            @NonNull User user,
            @NonNull ActivityType activityType,
            String description,
            String ipAddress,  // Nullable
            String deviceId,   // Nullable
            String sessionId) { // Nullable
        
        UserActivityHistory activity = UserActivityHistory.builder()
                .user(user)
                .username(user.getFullName())
                .userGroup(user.getRole() != null ? user.getRole().getName() : "Unknown")
                .activityTypeCode(activityType.getCode())
                .activityTypeName(activityType.getDisplayName())
                .description(description)
                .deviceId(deviceId != null ? deviceId : "unknown")
                .ip(ipAddress != null ? ipAddress : "0.0.0.0")
                .sessionId(sessionId != null ? sessionId : "")
                .activityDate(LocalDateTime.now())
                .createdDate(LocalDateTime.now())
                .createdBy(user)  
                .build();
        
        UserActivityHistory saved = activityHistoryRepository.save(activity);
        return Objects.requireNonNull(saved, "Saved activity should not be null");
    }


        public List<UserActivityHistoryResponseDTO> getAllActivityHistory() {
                return getAllActivities();
        }
    
    
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivityHistory(@NonNull Long userId) {
        List<UserActivityHistory> activities = activityHistoryRepository.findByUserId(userId);
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivitiesByDateRange(
            @NonNull Long userId,
            @NonNull LocalDateTime startDate,
            @NonNull LocalDateTime endDate) {
        
        List<UserActivityHistory> activities = activityHistoryRepository
                .findByUserIdAndActivityDateBetweenOrderByActivityDateDesc(userId, startDate, endDate);
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
   
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivitiesByType(
            @NonNull String activityTypeCode) {
        
        List<UserActivityHistory> activities = activityHistoryRepository
                .findByActivityTypeCodeOrderByActivityDateDesc(activityTypeCode);
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivitiesBySession(
            @NonNull String sessionId) {
        
        List<UserActivityHistory> activities = activityHistoryRepository
                .findBySessionIdOrderByActivityDateAsc(sessionId);
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getActivitiesByUserGroup(
            @NonNull String userGroup) {
        
        List<UserActivityHistory> activities = activityHistoryRepository
                .findByUserGroupOrderByActivityDateDesc(userGroup);
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
   
    @Transactional(readOnly = true)
    public long countActivities(@NonNull Long userId) {
        return activityHistoryRepository.countByUserId(userId);
    }
    
    
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getRecentActivities(
            @NonNull Long userId, 
            int days) {
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = now.minusDays(days);
        LocalDateTime endDate = now;
        
        return getActivitiesByDateRange(
            userId,
            Objects.requireNonNull(startDate, "startDate should not be null"),
            Objects.requireNonNull(endDate, "endDate should not be null"));
    }
    
    
    @Transactional(readOnly = true)
    public List<UserActivityHistoryResponseDTO> getAllActivities() {
        List<UserActivityHistory> activities = activityHistoryRepository
                .findByActivityDateBetweenOrderByActivityDateDesc(
                    LocalDateTime.of(2000, 1, 1, 0, 0),
                    LocalDateTime.now()
                );
        
        return activities.stream()
                .map(activityHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
}
