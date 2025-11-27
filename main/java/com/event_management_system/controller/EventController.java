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

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping
    @ApiOperation(value = "Create a new event", notes = "Creates a new event with provided details. The start time must be before end time.")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Event created successfully"),
        @ApiResponse(code = 400, message = "Invalid input data or validation failed"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<EventResponseDTO> createEvent(
            @ApiParam(value = "Event details to be created", required = true)
            @Valid @RequestBody EventRequestDTO eventRequestDTO) {
        EventResponseDTO savedEvent = eventService.createEvent(eventRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvent);
    }

    @GetMapping
    @ApiOperation(value = "Get all events", notes = "Retrieves a paginated list of all events in system")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Events retrieved successfully"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<Page<EventResponseDTO>> getAllEvents(
            @ApiParam(value = "Page number (0-based)", example = "0") 
            @RequestParam(defaultValue = "0") int page,
            @ApiParam(value = "Number of items per page", example = "10") 
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EventResponseDTO> events = eventService.getAllEvents(pageable);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{id}")
    @ApiOperation(value = "Get event by ID", notes = "Retrieves a specific event by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Event found and retrieved successfully"),
        @ApiResponse(code = 404, message = "Event not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<EventResponseDTO> getEventById(
            @ApiParam(value = "Unique identifier of event", required = true, example = "1")
            @PathVariable Long id) {
        Optional<EventResponseDTO> event = eventService.getEventById(id);
        if (event.isPresent()) {
            return ResponseEntity.ok(event.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}")
    @ApiOperation(value = "Update an existing event", notes = "Updates details of an existing event. The event must exist.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Event updated successfully"),
        @ApiResponse(code = 400, message = "Invalid input data or validation failed"),
        @ApiResponse(code = 404, message = "Event not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<EventResponseDTO> updateEvent(
            @ApiParam(value = "Unique identifier of event to update", required = true, example = "1")
            @PathVariable Long id, 
            @ApiParam(value = "Updated event details", required = true)
            @Valid @RequestBody EventRequestDTO eventDetails) {
        Optional<EventResponseDTO> eventOptional = eventService.updateEvent(id, eventDetails);
        if (eventOptional.isPresent()) {
            return ResponseEntity.ok(eventOptional.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("/{id}")
    @ApiOperation(value = "Delete an event", notes = "Soft-deletes an event by marking it as deleted. The event remains in the database but is marked as inactive.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Event deleted successfully"),
        @ApiResponse(code = 404, message = "Event not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public ResponseEntity<Void> deleteEvent(
            @ApiParam(value = "Unique identifier of event to delete", required = true, example = "1")
            @PathVariable Long id) {
        boolean deleted = eventService.deleteEvent(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
