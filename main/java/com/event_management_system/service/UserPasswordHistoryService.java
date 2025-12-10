package com.event_management_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.dto.UserPasswordHistoryResponseDTO;
import com.event_management_system.entity.User;
import com.event_management_system.entity.UserPasswordHistory;
import com.event_management_system.mapper.UserPasswordHistoryMapper;
import com.event_management_system.repository.UserPasswordHistoryRepository;

/**
 * UserPasswordHistoryService
 * 
 * Manages password change history tracking
 * 
 * Key Functions:
 * - Record every password change
 * - Retrieve password change history
 * - Count password changes
 * - Get recent password changes
 * 
 * Used by:
 * - AuthService (when user changes password)
 * - UserService (when admin resets password)
 * - HistoryController (to show password change history)
 */
@Service
public class UserPasswordHistoryService {
    
    @Autowired
    private UserPasswordHistoryRepository passwordHistoryRepository;
    
    @Autowired
    private UserPasswordHistoryMapper passwordHistoryMapper;
    
    /**
     * Record a password change event
     * 
     * Usage:
     * userPasswordHistoryService.recordPasswordChange(
     *     user, changedByUser, oldPasswordHash, newPasswordHash
     * );
     * 
     * @param user - The user whose password was changed
     * @param changedBy - Who changed it (self or admin)
     * @param oldPassword - Old password hash (can be null for new users)
     * @param newPassword - New password hash
     * @return Saved UserPasswordHistory entity
     */
    @Transactional
    public UserPasswordHistory recordPasswordChange(
            @NonNull User user,
            @NonNull User changedBy,
            String oldPassword,  // Nullable for new user creation
            @NonNull String newPassword) {
        
        UserPasswordHistory history = UserPasswordHistory.builder()
                .user(user)
                .passwordChangedBy(changedBy)
                .changeDate(LocalDateTime.now())
                .oldPassword(oldPassword)  // Can be null for new users
                .newPassword(newPassword)
                .createdBy(changedBy)  // User object, not String
                .build();
        
        UserPasswordHistory saved = passwordHistoryRepository.save(history);
        return Objects.requireNonNull(saved, "Saved password history should not be null");
    }
    
    /**
     * Get all password change history for a user
     * 
     * Usage:
     * List<UserPasswordHistoryResponseDTO> history = 
     *     userPasswordHistoryService.getPasswordHistory(userId);
     * 
     * @param userId - User ID
     * @return List of password change records (sorted newest first)
     */
    @Transactional(readOnly = true)
    public List<UserPasswordHistoryResponseDTO> getPasswordHistory(@NonNull Long userId) {
        List<UserPasswordHistory> history = passwordHistoryRepository.findByUserId(userId);
        return history.stream()
                .map(passwordHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get recent password changes within specified days
     * 
     * Usage:
     * // Get password changes in last 30 days
     * List<UserPasswordHistoryResponseDTO> recentChanges = 
     *     userPasswordHistoryService.getRecentPasswordChanges(userId, 30);
     * 
     * @param userId - User ID
     * @param days - Number of days to look back
     * @return List of recent password change records
     */
    @Transactional(readOnly = true)
    public List<UserPasswordHistoryResponseDTO> getRecentPasswordChanges(
            @NonNull Long userId, 
            int days) {
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();
        
        List<UserPasswordHistory> history = passwordHistoryRepository
                .findByUserIdAndChangeDateBetweenOrderByChangeDateDesc(userId, startDate, endDate);
        
        return history.stream()
                .map(passwordHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the most recent password change for a user
     * 
     * Usage:
     * UserPasswordHistoryResponseDTO lastChange = 
     *     userPasswordHistoryService.getLastPasswordChange(userId);
     * 
     * @param userId - User ID
     * @return Most recent password change record, or null if none
     */
    @Transactional(readOnly = true)
    public UserPasswordHistoryResponseDTO getLastPasswordChange(@NonNull Long userId) {
        return passwordHistoryRepository.findFirstByUserIdOrderByChangeDateDesc(userId)
                .map(passwordHistoryMapper::toDto)
                .orElse(null);
    }
    
    /**
     * Count total password changes for a user
     * 
     * Usage:
     * long count = userPasswordHistoryService.countPasswordChanges(userId);
     * 
     * @param userId - User ID
     * @return Total number of password changes
     */
    @Transactional(readOnly = true)
    public long countPasswordChanges(@NonNull Long userId) {
        return passwordHistoryRepository.countByUserId(userId);
    }
}
