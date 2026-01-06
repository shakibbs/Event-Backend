package com.event_management_system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteAttendeeRequestDTO {
    
   
    @NotNull(message = "User ID is required")
    private Long userId;
    

}
