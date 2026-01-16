package com.event_management_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.dto.UserLoginLogoutHistoryResponseDTO;
import com.event_management_system.entity.User;
import com.event_management_system.entity.UserLoginLogoutHistory;
import com.event_management_system.mapper.UserLoginLogoutHistoryMapper;
import com.event_management_system.repository.UserLoginLogoutHistoryRepository;


@Service
public class UserLoginLogoutHistoryService {
    
    @Autowired
    private UserLoginLogoutHistoryRepository loginHistoryRepository;
    
    @Autowired
    private UserLoginLogoutHistoryMapper loginHistoryMapper;
    
    
    @Transactional
    public UserLoginLogoutHistory recordLogin(
            @NonNull User user,
            @NonNull String userToken,
            @NonNull String requestIp,
            @NonNull String deviceInfo,
            @NonNull String loginStatus) {
        
        UserLoginLogoutHistory history = UserLoginLogoutHistory.builder()
                .user(user)
                .userToken(userToken)
                .userType(user.getRole() != null ? user.getRole().getName() : "UNKNOWN")
                .loginTime(LocalDateTime.now())
                .logoutTime(null)  // Not logged out yet
                .requestIp(requestIp)
                .deviceInfo(deviceInfo)
                .loginStatus(loginStatus)
                .build();
        
        UserLoginLogoutHistory saved = loginHistoryRepository.save(history);
        return Objects.requireNonNull(saved, "Saved login history should not be null");
    }
    
    
    @Transactional
    public boolean recordLogout(@NonNull String userToken) {
        return loginHistoryRepository.findByUserToken(userToken)
                .map(history -> {
                    history.setLogoutTime(LocalDateTime.now());
                    loginHistoryRepository.save(history);
                    return true;
                })
                .orElse(false);
    }
    
    
    @Transactional(readOnly = true)
    public List<UserLoginLogoutHistoryResponseDTO> getLoginHistory(@NonNull Long userId) {
        List<UserLoginLogoutHistory> history = loginHistoryRepository.findByUserId(userId);
        return history.stream()
                .map(loginHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    
    @Transactional(readOnly = true)
    public List<UserLoginLogoutHistoryResponseDTO> getActiveSessions(@NonNull Long userId) {
        List<UserLoginLogoutHistory> activeSessions = 
                loginHistoryRepository.findByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(userId);
        
        return activeSessions.stream()
                .map(loginHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    
    @Transactional(readOnly = true)
    public long countActiveSessions(@NonNull Long userId) {
        return loginHistoryRepository.countByUserIdAndLogoutTimeIsNull(userId);
    }
    
    
    @Transactional(readOnly = true)
    public List<UserLoginLogoutHistoryResponseDTO> getLoginHistoryByDateRange(
            @NonNull Long userId,
            @NonNull LocalDateTime startDate,
            @NonNull LocalDateTime endDate) {
        
        List<UserLoginLogoutHistory> history = loginHistoryRepository
                .findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(userId, startDate, endDate);
        
        return history.stream()
                .map(loginHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<UserLoginLogoutHistoryResponseDTO> getLoginHistoryByStatus(
            @NonNull Long userId,
            @NonNull String loginStatus) {
        
        List<UserLoginLogoutHistory> history = loginHistoryRepository
                .findByUserIdAndLoginStatusOrderByLoginTimeDesc(userId, loginStatus);
        
        return history.stream()
                .map(loginHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
    
   
    @Transactional(readOnly = true)
    public UserLoginLogoutHistoryResponseDTO getLastLogin(@NonNull Long userId) {
        return loginHistoryRepository.findFirstByUserIdOrderByLoginTimeDesc(userId)
                .map(loginHistoryMapper::toDto)
                .orElse(null);
    }
    
    
    @Transactional
    public boolean forceLogout(@NonNull String userToken) {
        return loginHistoryRepository.findByUserToken(userToken)
                .map(history -> {
                    history.setLogoutTime(LocalDateTime.now());
                    history.setLoginStatus("FORCE_LOGOUT");
                    loginHistoryRepository.save(history);
                    return true;
                })
                .orElse(false);
    }
}
