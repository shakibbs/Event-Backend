
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


@Service
public class UserPasswordHistoryService {

    
    
    @Autowired
    private UserPasswordHistoryRepository passwordHistoryRepository;
    
    @Autowired
    private UserPasswordHistoryMapper passwordHistoryMapper;
    
    
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
    
    
    @Transactional(readOnly = true)
    public List<UserPasswordHistoryResponseDTO> getPasswordHistory(@NonNull Long userId) {
        List<UserPasswordHistory> history = passwordHistoryRepository.findByUserId(userId);
        return history.stream()
                .map(passwordHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    

    @Transactional(readOnly = true)
    public List<UserPasswordHistoryResponseDTO> getAllPasswordHistory() {
        List<UserPasswordHistory> history = passwordHistoryRepository.findAll();
        return history.stream()
                .map(passwordHistoryMapper::toDto)
                .collect(Collectors.toList());
    }

        @Transactional
        public void deleteAllByUserId(Long userId) {
            passwordHistoryRepository.deleteAll(passwordHistoryRepository.findByUserId(userId));
        }
  
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
    
    
    @Transactional(readOnly = true)
    public UserPasswordHistoryResponseDTO getLastPasswordChange(@NonNull Long userId) {
        return passwordHistoryRepository.findFirstByUserIdOrderByChangeDateDesc(userId)
                .map(passwordHistoryMapper::toDto)
                .orElse(null);
    }
    
 
    @Transactional(readOnly = true)
    public long countPasswordChanges(@NonNull Long userId) {
        return passwordHistoryRepository.countByUserId(userId);
    }
}
