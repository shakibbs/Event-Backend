package com.event_management_system.dto;

import java.util.List;

// Unified DTO for all history types
public class AllHistoryResponseDTO {
    private List<UserActivityHistoryResponseDTO> activities;
    private List<UserLoginLogoutHistoryResponseDTO> logins;
    private List<UserPasswordHistoryResponseDTO> passwords;

    public AllHistoryResponseDTO() {}

    public AllHistoryResponseDTO(List<UserActivityHistoryResponseDTO> activities,
                                 List<UserLoginLogoutHistoryResponseDTO> logins,
                                 List<UserPasswordHistoryResponseDTO> passwords) {
        this.activities = activities;
        this.logins = logins;
        this.passwords = passwords;
    }

    public List<UserActivityHistoryResponseDTO> getActivities() {
        return activities;
    }
    public void setActivities(List<UserActivityHistoryResponseDTO> activities) {
        this.activities = activities;
    }
    public List<UserLoginLogoutHistoryResponseDTO> getLogins() {
        return logins;
    }
    public void setLogins(List<UserLoginLogoutHistoryResponseDTO> logins) {
        this.logins = logins;
    }
    public List<UserPasswordHistoryResponseDTO> getPasswords() {
        return passwords;
    }
    public void setPasswords(List<UserPasswordHistoryResponseDTO> passwords) {
        this.passwords = passwords;
    }
}
