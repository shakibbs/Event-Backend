package com.event_management_system.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
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
import com.event_management_system.entity.User;
import com.event_management_system.repository.UserRepository;
import com.event_management_system.service.EventService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Event Management", description = "APIs for managing events in the system")
public class EventController {

    @Autowired
    private EventService eventService;
    
    @Autowired
    private UserRepository userRepository;

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
            @Valid @RequestBody @NonNull EventRequestDTO eventRequestDTO,
            Authentication authentication) {
        
        // Get current user ID from authentication
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        EventResponseDTO savedEvent = eventService.createEvent(eventRequestDTO, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvent);
    }

    @GetMapping
    @Operation(summary = "Get all events", description = "Retrieves a paginated list of all events visible to the current user based on their role and permissions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Events retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Page<EventResponseDTO>> getAllEvents(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        
        // Validate pagination parameters
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be 0 or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Get all events the user can view
        List<EventResponseDTO> allVisibleEvents = eventService.getEventsForUser(currentUser.getId());
        
        // Apply pagination
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, allVisibleEvents.size());
        
        if (startIndex >= allVisibleEvents.size()) {
            // Return empty page if start index is beyond the list
            Page<EventResponseDTO> emptyPage = new org.springframework.data.domain.PageImpl<>(
                    java.util.Collections.emptyList(),
                    PageRequest.of(page, size),
                    allVisibleEvents.size());
            return ResponseEntity.ok(emptyPage);
        }
        
        List<EventResponseDTO> paginatedEvents = allVisibleEvents.subList(startIndex, endIndex);
        Page<EventResponseDTO> eventPage = new org.springframework.data.domain.PageImpl<>(
                paginatedEvents,
                PageRequest.of(page, size),
                allVisibleEvents.size());
        
        return ResponseEntity.ok(eventPage);
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
            @PathVariable @NonNull Long id,
            Authentication authentication) {
        
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        Optional<EventResponseDTO> event = eventService.getEventById(id, currentUser.getId());
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
            @PathVariable @NonNull Long id,
            @Parameter(description = "Updated event details", required = true)
            @Valid @RequestBody @NonNull EventRequestDTO eventDetails,
            Authentication authentication) {
        
        String email = authentication.getName();
        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        Optional<EventResponseDTO> eventOptional = eventService.updateEvent(id, eventDetails, currentUser.getId());
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
            @PathVariable @NonNull Long id,
            Authentication authentication) {
        
        try {
            String email = authentication.getName();
            User currentUser = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
            boolean deleted = eventService.deleteEvent(id, currentUser.getId());
            if (deleted) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
