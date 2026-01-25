package com.event_management_system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.event_management_system.dto.EventRequestDTO;
import com.event_management_system.dto.EventResponseDTO;
import com.event_management_system.entity.User;
import com.event_management_system.exception.GlobalExceptionHandler.BadRequestException;
import com.event_management_system.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.event_management_system.repository.UserRepository;
import com.event_management_system.service.ApplicationLoggerService;
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

        @Autowired
        private ApplicationLoggerService log;



        @PostMapping
        @Operation(summary = "Create a new event", description = "Creates a new event with provided details. The start time must be before end time.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Event created successfully", content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data or validation failed"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<EventResponseDTO> createEvent(
                        @Parameter(description = "Event details to be created", required = true) @Valid @RequestBody @NonNull EventRequestDTO eventRequestDTO,
                        Authentication authentication) {

                try {
                        log.trace("createEvent() called with title={}, startTime={}, endTime={}, timestamp={}",
                                        eventRequestDTO.getTitle(), eventRequestDTO.getStartTime(),
                                        eventRequestDTO.getEndTime(), System.currentTimeMillis());

                        log.debug("POST /api/events - Creating event: title={}, location={}",
                                        eventRequestDTO.getTitle(), eventRequestDTO.getLocation());

                        String email = authentication.getName();
                        User currentUser = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "User not found with email: " + email));

                        log.debug("User authenticated: userId={}, email={}", currentUser.getId(), email);

                        EventResponseDTO savedEvent = eventService.createEvent(eventRequestDTO, currentUser.getId());

                        log.info("Event created successfully: eventId={}, title={}, userId={}",
                                        savedEvent.getId(), savedEvent.getTitle(), currentUser.getId());

                        return ResponseEntity.status(HttpStatus.CREATED).body(savedEvent);
                } catch (Exception e) {
                        log.error("Failed to create event", e);
                        throw e;
                }
        }

        @GetMapping
        @Operation(summary = "Get all events", description = "Retrieves a paginated list of all events visible to the current user based on their role and permissions")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Events retrieved successfully", content = @Content(schema = @Schema(implementation = Page.class))),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<Page<EventResponseDTO>> getAllEvents(
                        @Parameter(description = "Page number (0-based)", example = "0") @RequestParam(defaultValue = "0") int page,
                        @Parameter(description = "Number of items per page", example = "10") @RequestParam(defaultValue = "10") int size,
                        Authentication authentication) {

                try {
                        log.trace("getAllEvents() called with page={}, size={}, timestamp={}",
                                        page, size, System.currentTimeMillis());

                        log.debug("GET /api/events - Fetching events: page={}, size={}", page, size);

                        if (page < 0) {
                                throw new BadRequestException("Page number must be 0 or greater");
                        }
                        if (size < 1 || size > 100) {
                                throw new BadRequestException("Page size must be between 1 and 100");
                        }

                        String email = authentication.getName();
                        User currentUser = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "User not found with email: " + email));

                        log.debug("User authenticated: userId={}, email={}", currentUser.getId(), email);

                        List<EventResponseDTO> allVisibleEvents = eventService.getEventsForUser(currentUser.getId());

                        int startIndex = page * size;
                        int endIndex = Math.min(startIndex + size, allVisibleEvents.size());

                        List<EventResponseDTO> paginatedEvents;
                        if (startIndex >= allVisibleEvents.size()) {
                                paginatedEvents = java.util.Collections.emptyList();
                        } else {
                                paginatedEvents = allVisibleEvents.subList(startIndex, endIndex);
                        }

                        Page<EventResponseDTO> eventPage = new org.springframework.data.domain.PageImpl<>(
                                        paginatedEvents,
                                        PageRequest.of(page, size),
                                        allVisibleEvents.size());

                        log.info("Events retrieved successfully: totalEvents={}, page={}, size={}, userId={}",
                                        allVisibleEvents.size(), page, size, currentUser.getId());

                        return ResponseEntity.ok(eventPage);
                } catch (Exception e) {
                        log.error("Failed to retrieve events", e);
                        throw e;
                }
        }


         @GetMapping("/public")
        public ResponseEntity<List<EventResponseDTO>> getPublicUpcomingEvents() {
                List<EventResponseDTO> events = eventService.findPublicUpcomingEvents();
                return ResponseEntity.ok(events);
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get event by ID", description = "Retrieves a specific event by its unique identifier")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Event found and retrieved successfully", content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
                        @ApiResponse(responseCode = "404", description = "Event not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<EventResponseDTO> getEventById(
                        @Parameter(description = "Unique identifier of event", required = true, example = "1") @PathVariable @NonNull Long id,
                        Authentication authentication) {

                try {
                        log.trace("getEventById() called with eventId={}, timestamp={}",
                                        id, System.currentTimeMillis());

                        log.debug("GET /api/events/{} - Fetching event by ID", id);

                        String email = authentication.getName();
                        User currentUser = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "User not found with email: " + email));

                        log.debug("User authenticated: userId={}, email={}", currentUser.getId(), email);

                        EventResponseDTO event = eventService.getEventById(id, currentUser.getId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Event not found with id: " + id));

                        log.info("Event retrieved successfully: eventId={}, userId={}", id, currentUser.getId());

                        return ResponseEntity.ok(event);
                } catch (Exception e) {
                        log.error("Failed to retrieve event: eventId={}", id, e);
                        throw e;
                }
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update an existing event", description = "Updates details of an existing event. The event must exist.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Event updated successfully", content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid input data or validation failed"),
                        @ApiResponse(responseCode = "404", description = "Event not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<EventResponseDTO> updateEvent(
                        @Parameter(description = "Unique identifier of event to update", required = true, example = "1") @PathVariable @NonNull Long id,
                        @Parameter(description = "Updated event details", required = true) @Valid @RequestBody @NonNull EventRequestDTO eventDetails,
                        Authentication authentication) {

                try {
                        log.trace("updateEvent() called with eventId={}, title={}, startTime={}, timestamp={}",
                                        id, eventDetails.getTitle(), eventDetails.getStartTime(),
                                        System.currentTimeMillis());

                        log.debug("PUT /api/events/{} - Updating event: title={}", id, eventDetails.getTitle());

                        String email = authentication.getName();
                        User currentUser = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "User not found with email: " + email));

                        log.debug("User authenticated: userId={}, email={}", currentUser.getId(), email);

                        EventResponseDTO updatedEvent = eventService.updateEvent(id, eventDetails, currentUser.getId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Event not found with id: " + id));

                        log.info("Event updated successfully: eventId={}, title={}, userId={}",
                                        id, updatedEvent.getTitle(), currentUser.getId());

                        return ResponseEntity.ok(updatedEvent);
                } catch (Exception e) {
                        log.error("Failed to update event: eventId={}", id, e);
                        throw e;
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
                        @Parameter(description = "Unique identifier of event to delete", required = true, example = "1") @PathVariable @NonNull Long id,
                        Authentication authentication) {

                try {
                        log.trace("deleteEvent() called with eventId={}, timestamp={}",
                                        id, System.currentTimeMillis());

                        log.debug("DELETE /api/events/{} - Deleting event", id);

                        String email = authentication.getName();
                        User currentUser = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "User not found with email: " + email));

                        log.debug("User authenticated: userId={}, email={}", currentUser.getId(), email);

                        boolean deleted = eventService.deleteEvent(id, currentUser.getId());

                        if (!deleted) {
                                throw new ResourceNotFoundException("Event not found with id: " + id);
                        }

                        log.info("Event deleted successfully: eventId={}, userId={}", id, currentUser.getId());

                        return ResponseEntity.ok().build();
                } catch (Exception e) {
                        log.error("Failed to delete event: eventId={}", id, e);
                        throw e;
                }
        }

            @GetMapping("/{eventId}/attendees")
            @Operation(summary = "Get attendees for an event", description = "Returns the list of attendees for a given event, including invitation status.")
                        public ResponseEntity<List<com.event_management_system.dto.EventAttendeeDTO>> getEventAttendees(
                                        @PathVariable Long eventId,
                                        Authentication authentication) {
                                String email = authentication.getName();
                                User currentUser = userRepository.findByEmail(email)
                                                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

                                List<com.event_management_system.entity.EventAttendees> attendees = eventService.getAttendeesForEvent(eventId, currentUser.getId());
                                // Map entities to DTOs
                                List<com.event_management_system.dto.EventAttendeeDTO> attendeeDTOs = attendees.stream().map(att -> {
                                        com.event_management_system.dto.EventAttendeeDTO dto = new com.event_management_system.dto.EventAttendeeDTO();
                                        dto.setId(att.getId());
                                        dto.setEmail(att.getEmail());
                                        dto.setInvitationStatus(att.getInvitationStatus());
                                        dto.setInvitationToken(att.getInvitationToken());
                                        dto.setInvitationSentAt(att.getInvitationSentAt());
                                        dto.setResponseAt(att.getResponseAt());
                                        dto.setAdvanceReminderSent(att.getAdvanceReminderSent());
                                        dto.setLastMinuteReminderSent(att.getLastMinuteReminderSent());
                                        if (att.getUser() != null) {
                                                dto.setUserId(att.getUser().getId());
                                                dto.setUserFullName(att.getUser().getFullName());
                                        }
                                        return dto;
                                }).toList();
                                return ResponseEntity.ok(attendeeDTOs);
                        }

        @PostMapping(value = "/{eventId}/invite", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Invite users to event", description = "Invite users via CSV file and broadcast to all registered users. Send as multipart/form-data.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invitations processing started"),
                        @ApiResponse(responseCode = "400", description = "Invalid request or file"),
                        @ApiResponse(responseCode = "403", description = "Not authorized"),
                        @ApiResponse(responseCode = "404", description = "Event not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<?> inviteUsers(
                        @PathVariable Long eventId,
                        @RequestPart(value = "file", required = false) MultipartFile file,
                        Authentication authentication) {

                try {
                        log.trace("inviteUsers() called with eventId={}, timestamp={}", eventId,
                                        System.currentTimeMillis());

                        String email = authentication.getName();
                        User organizer = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

                        java.util.Map<String, Object> result = eventService.sendBulkInvitations(eventId, file, organizer.getId());

                        return ResponseEntity.ok().body(result);
                } catch (Exception e) {
                        log.error("Failed to process invitations: eventId={}", eventId, e);
                        throw e;
                }
        }

        @PostMapping("/{eventId}/action")
        @Operation(summary = "Perform action approval/rejection on an event", description = "Approve or Reject an event. Action logic is unified. Requires event.approve permission. Remarks mandatory for Rejection.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Action performed successfully", content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
                        @ApiResponse(responseCode = "400", description = "Invalid action, missing remarks, or self-approval attempt"),
                        @ApiResponse(responseCode = "403", description = "No permission to perform action"),
                        @ApiResponse(responseCode = "404", description = "Event or user not found"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<EventResponseDTO> performEventAction(
                        @Parameter(description = "Unique identifier of the event", required = true, example = "1") @PathVariable @NonNull Long eventId,
                        @Parameter(description = "Action details (APPROVE/REJECT + remarks)", required = true) @RequestBody @NonNull com.event_management_system.dto.EventActionRequestDTO actionRequest,
                        Authentication authentication) {

                try {
                        log.trace("performEventAction() called with eventId={}, action={}, timestamp={}",
                                        eventId, actionRequest.getAction(), System.currentTimeMillis());

                        log.debug("POST /api/events/{}/action - Performing event action", eventId);

                        String email = authentication.getName();
                        User checker = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "User not found with email: " + email));

                        log.debug("Checker authenticated: checkerId={}, email={}", checker.getId(), email);

                        EventResponseDTO response = eventService.processEventAction(eventId, actionRequest,
                                        checker.getId());

                        log.info("Event action performed successfully: eventId={}, checkerId={}, action={}",
                                        eventId, checker.getId(), actionRequest.getAction());

                        return ResponseEntity.ok(response);
                } catch (Exception e) {
                        log.error("Failed to perform event action: eventId={}", eventId, e);
                        throw e;
                }
        }

        @GetMapping("/respond")
        @Operation(summary = "Respond to event invitation", description = "Allows invitees to accept or decline an event invitation using the token from their email. No authentication required.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Response recorded successfully"),
                        @ApiResponse(responseCode = "400", description = "Invalid token or action, or already responded"),
                        @ApiResponse(responseCode = "404", description = "Invalid invitation token"),
                        @ApiResponse(responseCode = "500", description = "Internal server error")
        })
        public ResponseEntity<String> respondToInvitation(
                        @Parameter(description = "Invitation token from email", required = true) @RequestParam @NonNull String token,
                        @Parameter(description = "Action to take: ACCEPT or DECLINE", required = true) @RequestParam @NonNull String action) {

                try {
                        log.trace("respondToInvitation() called with token={}, action={}, timestamp={}",
                                        token, action, System.currentTimeMillis());

                        log.debug("GET /api/events/respond - Processing invitation response: action={}", action);

                        eventService.respondToInvitation(token, action);

                        String message = "ACCEPT".equalsIgnoreCase(action)
                                        ? "Thank you! You have successfully accepted the invitation."
                                        : "You have declined the invitation.";

                        log.info("Invitation response recorded: token={}, action={}", token, action);

                        return ResponseEntity.ok(message);
                } catch (Exception e) {
                        log.error("Failed to process invitation response: token={}", token, e);
                        throw e;
                }
        }

        @PostMapping("/{eventId}/hold")
        @Operation(summary = "Hold an event", description = "Temporarily pause an event (SuperAdmin only). Sets event status to INACTIVE.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Event held successfully"),
                        @ApiResponse(responseCode = "400", description = "Cannot hold event in current status"),
                        @ApiResponse(responseCode = "404", description = "Event not found")
        })
        public ResponseEntity<String> holdEvent(
                        @Parameter(description = "Event ID to hold", required = true) @PathVariable @NonNull Long eventId,
                        Authentication authentication) {

                try {
                        log.trace("holdEvent() called with eventId={}, timestamp={}", eventId,
                                        System.currentTimeMillis());
                        log.debug("POST /api/events/{}/hold - Holding event", eventId);

                        String email = authentication.getName();
                        User currentUser = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "User not found with email: " + email));

                        eventService.holdEvent(eventId, currentUser.getId());

                        log.info("Event held successfully: eventId={}, userId={}", eventId, currentUser.getId());
                        return ResponseEntity.ok("Event has been put on hold");
                } catch (Exception e) {
                        log.error("Failed to hold event: eventId={}", eventId, e);
                        throw e;
                }
        }

        @PostMapping("/{eventId}/reactivate")
        @Operation(summary = "Reactivate a held event", description = "Resume a held event (SuperAdmin only). Sets event status back to ACTIVE.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Event reactivated successfully"),
                        @ApiResponse(responseCode = "400", description = "Event is not in INACTIVE status"),
                        @ApiResponse(responseCode = "404", description = "Event not found")
        })
        public ResponseEntity<String> reactivateEvent(
                        @Parameter(description = "Event ID to reactivate", required = true) @PathVariable @NonNull Long eventId,
                        Authentication authentication) {

                try {
                        log.trace("reactivateEvent() called with eventId={}, timestamp={}", eventId,
                                        System.currentTimeMillis());
                        log.debug("POST /api/events/{}/reactivate - Reactivating event", eventId);

                        String email = authentication.getName();
                        User currentUser = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "User not found with email: " + email));

                        eventService.reactivateEvent(eventId, currentUser.getId());

                        log.info("Event reactivated successfully: eventId={}, userId={}", eventId, currentUser.getId());
                        return ResponseEntity.ok("Event has been reactivated");
                } catch (Exception e) {
                        log.error("Failed to reactivate event: eventId={}", eventId, e);
                        throw e;
                }
        }
}
