package com.event_management_system.dto;

import com.event_management_system.entity.BaseEntity;
import com.event_management_system.entity.Event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {

    private Long id;
    private String title;
    private String description;
    
    @Schema(example = "2025-12-20 10:00:00", description = "Event start time", type = "string")
    private String startTime;
    
    @Schema(example = "2025-12-20 17:00:00", description = "Event end time", type = "string")
    private String endTime;
    
    private String location;
    
    @Schema(example = "PUBLIC", description = "Event visibility level (PUBLIC, PRIVATE, INVITE_ONLY)")
    private Event.Visibility visibility;
    
    @Schema(example = "2025-12-10 15:30:45")
    private String createdAt;
    
    @Schema(example = "2025-12-10 15:30:45")
    private String updatedAt;
    
    private String createdBy;
    private String updatedBy;
    private BaseEntity.Status status;
    private Boolean deleted;
}