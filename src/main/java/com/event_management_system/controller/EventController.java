package com.event_management_system.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.event_management_system.dto.EventRequestDTO;
import com.event_management_system.dto.EventResponseDTO;
import com.event_management_system.service.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping
    @Operation(summary = "Create a new event", description = "Creates a new event with provided details. The start time must be before end time.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Event created successfully",
                    content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or validation failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<EventResponseDTO> createEvent(
            @Parameter(description = "Event details to be created", required = true)
            @Valid @RequestBody EventRequestDTO eventRequestDTO) {
        EventResponseDTO savedEvent = eventService.createEvent(eventRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvent);
    }

    @GetMapping
    @Operation(summary = "Get all events", description = "Retrieves a paginated list of all events in system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Events retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<EventResponseDTO>> getAllEvents(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponseDTO> events = eventService.getAllEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID", description = "Retrieves a specific event by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event found and retrieved successfully",
                    content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<EventResponseDTO> getEventById(
            @Parameter(description = "Unique identifier of event", required = true, example = "1")
            @PathVariable Long id) {
        Optional<EventResponseDTO> event = eventService.getEventById(id);
        if (event.isPresent()) {
            return ResponseEntity.ok(event.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing event", description = "Updates details of an existing event. The event must exist.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event updated successfully",
                    content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or validation failed"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<EventResponseDTO> updateEvent(
            @Parameter(description = "Unique identifier of event to update", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "Updated event details", required = true)
            @Valid @RequestBody EventRequestDTO eventDetails) {
        Optional<EventResponseDTO> eventOptional = eventService.updateEvent(id, eventDetails);
        if (eventOptional.isPresent()) {
            return ResponseEntity.ok(eventOptional.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event", description = "Soft-deletes an event by marking it as deleted. The event remains in the database but is marked as inactive.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Event deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Event not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteEvent(
            @Parameter(description = "Unique identifier of event to delete", required = true, example = "1")
            @PathVariable Long id) {
        boolean deleted = eventService.deleteEvent(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
