package com.event_management_system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventActionRequestDTO {

    @Schema(description = "Event ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Event ID is required")
    private Long eventId;

    public enum Action {
        APPROVE,
        REJECT
    }

    @Schema(description = "Action for event checker (APPROVE, REJECT). Required.", example = "APPROVE")
    @NotNull(message = "Action is required")
    private Action action;

    @Schema(description = "Remarks for the action. Mandatory if action is REJECT.", example = "Approved by admin")
    private String remarks;
}
