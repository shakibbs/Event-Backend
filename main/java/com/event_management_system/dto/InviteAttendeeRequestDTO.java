package com.event_management_system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for inviting an attendee to an event. Provide either userId (for registered users) or email (for external users).")
public class InviteAttendeeRequestDTO {

    @Schema(description = "User ID for registered users (optional if email is provided)")
    private Long userId;

    @Email(message = "Invalid email format")
    @Schema(description = "Email address for external users (optional if userId is provided)")
    private String email;

    @Schema(description = "Name for external users (optional, used when email is provided)")
    private String name;

    
    public boolean isValid() {
        return userId != null || (email != null && !email.trim().isEmpty());
    }

   
    public boolean isRegisteredUser() {
        return userId != null;
    }

    
    public boolean isExternalUser() {
        return userId == null && email != null && !email.trim().isEmpty();
    }
}
