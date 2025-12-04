package com.event_management_system.enums;

public enum EventVisibility {
    PUBLIC("Public"),
    PRIVATE("Private"),
    INVITE_ONLY("Invite Only");
    
    private final String displayName;
    
    EventVisibility(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}