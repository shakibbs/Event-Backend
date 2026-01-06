package com.event_management_system.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.dto.EventRequestDTO;
import com.event_management_system.dto.EventResponseDTO;
import com.event_management_system.entity.Event;
import com.event_management_system.mapper.EventMapper;
import com.event_management_system.repository.EventRepository;
import com.event_management_system.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EventService {

    @Autowired
    private ApplicationLoggerService logger;
    
    @Autowired
    private EventRepository eventRepository;
    
    @Autowired
    private EventMapper eventMapper;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserActivityHistoryService activityHistoryService;
    
    @Transactional
    public EventResponseDTO createEvent(@NonNull EventRequestDTO eventRequestDTO, @NonNull Long currentUserId) {
        // TRACE: Entry point
        logger.trace("[EventService] TRACE - createEvent() called with userId=" + currentUserId + ", title=" + eventRequestDTO.getTitle());
        
        // DEBUG: Check permission before creating event
        logger.debug("[EventService] DEBUG - createEvent() - Checking permissions for user " + currentUserId);
        boolean hasManageOwn = hasPermission(currentUserId, "event.manage.own");
        boolean hasManageAll = hasPermission(currentUserId, "event.manage.all");
        
        logger.debug("[EventService] DEBUG - createEvent() - hasPermission('event.manage.own'): " + hasManageOwn);
        logger.debug("[EventService] DEBUG - createEvent() - hasPermission('event.manage.all'): " + hasManageAll);
        
        if (!hasManageOwn && !hasManageAll) {
            logger.warn("[EventService] WARN - User " + currentUserId + " not authorized to create events");
            throw new RuntimeException("You don't have permission to create events");
        }
        
        if (!eventRequestDTO.isDateRangeValid()) {
            logger.warn("[EventService] WARN - Invalid date range: end time must be after start time");
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        if (!eventRequestDTO.areDatesInFuture()) {
            logger.warn("[EventService] WARN - Invalid dates: start and end times must be in the future");
            throw new IllegalArgumentException("Start time and end time must be in the future");
        }
        
        Event event = eventMapper.toEntity(eventRequestDTO);
        
        // Set organizer
        userRepository.findById(currentUserId).ifPresent(user -> {
            event.setOrganizer(user);
            logger.debug("[EventService] DEBUG - createEvent() - Set organizer to user " + user.getId());
        });
        
        event.recordCreation("system");
        Event savedEvent = eventRepository.save(event);
        
        // INFO: Event created successfully
        logger.info("[EventService] INFO - Event created successfully: eventId=" + savedEvent.getId() + ", title=" + savedEvent.getTitle() + ", userId=" + currentUserId);
        
        // Record activity: Event Created
        userRepository.findById(currentUserId).ifPresent(user -> {
            try {
                activityHistoryService.recordActivity(
                    Objects.requireNonNull(user, "User should not be null"),
                    com.event_management_system.entity.UserActivityHistory.ActivityType.EVENT_CREATED,
                    "Event: " + savedEvent.getTitle(),
                    "0.0.0.0",  // IP will be captured from controller if needed
                    "system",   // Device ID
                    java.util.UUID.randomUUID().toString()  // Session ID
                );
            } catch (Exception e) {
                logger.error("[EventService] ERROR - Failed to record event creation activity: " + e.getMessage());
            }
        });
        
        return eventMapper.toDto(savedEvent);
    }

    @Transactional(readOnly = true)
    public Optional<EventResponseDTO> getEventById(@NonNull Long id, @NonNull Long currentUserId) {
        return eventRepository.findById(id).map(event -> {
            if (!canViewEvent(event, currentUserId)) {
                throw new RuntimeException("You don't have permission to view this event");
            }
            return eventMapper.toDto(event);
        });
    }

    @Transactional
    public Optional<EventResponseDTO> updateEvent(@NonNull Long id, @NonNull EventRequestDTO eventRequestDTO, @NonNull Long currentUserId) {
        // TRACE: Entry point
        logger.trace("[EventService] TRACE - updateEvent() called with eventId=" + id + ", userId=" + currentUserId);
        
        // DEBUG: Check if user can manage this event
        logger.debug("[EventService] DEBUG - updateEvent() - Checking manage permission for event " + id);
        return eventRepository.findById(id).map(existingEvent -> {
            if (!canManageEvent(existingEvent, currentUserId)) {
                logger.warn("[EventService] WARN - User " + currentUserId + " not authorized to update event " + id);
                throw new RuntimeException("You don't have permission to update this event");
            }
            
            if (!eventRequestDTO.isDateRangeValid()) {
                logger.warn("[EventService] WARN - Invalid date range in update: end time must be after start time");
                throw new IllegalArgumentException("End time must be after start time");
            }
            
            if (!eventRequestDTO.areDatesInFuture()) {
                logger.warn("[EventService] WARN - Invalid future dates in update: start and end times must be in the future");
                throw new IllegalArgumentException("Start time and end time must be in the future");
            }
            
            logger.debug("[EventService] DEBUG - updateEvent() - Updating event entity with new data");
            eventMapper.updateEntity(eventRequestDTO, existingEvent);
           
            // Update audit fields
            existingEvent.recordUpdate("system");
            Event updatedEvent = eventRepository.save(existingEvent);
            
            // INFO: Event updated successfully
            logger.info("[EventService] INFO - Event updated successfully: eventId=" + updatedEvent.getId() + ", title=" + updatedEvent.getTitle() + ", userId=" + currentUserId);
            
            // Record activity: Event Updated
            userRepository.findById(currentUserId).ifPresent(user -> {
                try {
                    activityHistoryService.recordActivity(
                        Objects.requireNonNull(user, "User should not be null"),
                        com.event_management_system.entity.UserActivityHistory.ActivityType.EVENT_UPDATED,
                        "Event: " + updatedEvent.getTitle(),
                        "0.0.0.0",
                        "system",
                        java.util.UUID.randomUUID().toString()
                    );
                } catch (Exception e) {
                    logger.error("[EventService] ERROR - Failed to record event update activity: " + e.getMessage());
                }
            });
            
            return eventMapper.toDto(updatedEvent);
        });
    }

    @Transactional
    public boolean deleteEvent(@NonNull Long id, @NonNull Long currentUserId) {
        // TRACE: Entry point
        logger.trace("[EventService] TRACE - deleteEvent() called with eventId=" + id + ", userId=" + currentUserId);
        
        return eventRepository.findById(id).map(event -> {
            // DEBUG: Check if user can manage this event
            logger.debug("[EventService] DEBUG - deleteEvent() - Checking manage permission for event " + id);
            if (!canManageEvent(event, currentUserId)) {
                logger.warn("[EventService] WARN - User " + currentUserId + " not authorized to delete event " + id);
                throw new RuntimeException("You don't have permission to delete this event");
            }
            
            String eventTitle = event.getTitle();  // Save title before deletion
            logger.debug("[EventService] DEBUG - deleteEvent() - Marking event as deleted: " + eventTitle);
            event.markDeleted();
            eventRepository.save(event);
            
            // INFO: Event deleted successfully
            logger.info("[EventService] INFO - Event deleted successfully: eventId=" + event.getId() + ", title=" + eventTitle + ", userId=" + currentUserId);
            
            // Record activity: Event Deleted
            userRepository.findById(currentUserId).ifPresent(user -> {
                try {
                    activityHistoryService.recordActivity(
                        Objects.requireNonNull(user, "User should not be null"),
                        com.event_management_system.entity.UserActivityHistory.ActivityType.EVENT_DELETED,
                        "Event: " + eventTitle + " (ID: " + event.getId() + ")",
                        "0.0.0.0",
                        "system",
                        java.util.UUID.randomUUID().toString()
                    );
                } catch (Exception e) {
                    logger.error("[EventService] ERROR - Failed to record event deletion activity: " + e.getMessage());
                }
            });
            
            return true;
        }).orElse(false);
    }
    
    @Transactional(readOnly = true)
    public Page<EventResponseDTO> getAllEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findAllByDeletedFalse(pageable);
        return events.map(eventMapper::toDto);
    }
    
    @Transactional(readOnly = true)
    public List<EventResponseDTO> getAllEventsList() {
        List<Event> events = eventRepository.findAllByDeletedFalse();
        return events.stream()
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }

    // Helper methods for permission checking
    private boolean hasPermission(@NonNull Long userId, String permissionName) {
        return userService.hasPermission(userId, permissionName);
    }


    private boolean canViewEvent(Event event, @NonNull Long userId) {
        // SuperAdmin can view all events
        if (hasPermission(userId, "event.manage.all")) {
            return true;
        }

        // Admin can view all events
        if (hasPermission(userId, "event.view.all")) {
            return true;
        }

        // Event organizer can view their own events
        if (event.getOrganizer() != null && Objects.equals(event.getOrganizer().getId(), userId)) {
            return true;
        }

        // Check event visibility based on user permissions and event type
        if (event.getVisibility() != null) {
            return switch (event.getVisibility()) {
                case PUBLIC -> hasPermission(userId, "event.view.public");
                case PRIVATE -> false; // Private events can only be viewed by organizer and SuperAdmin
                case INVITE_ONLY -> {
                    // Check permission for invite-only events and user invitation
                    var user = userRepository.findById(userId).orElse(null);
                    yield hasPermission(userId, "event.view.invited") &&
                           (user != null && isUserInvitedToEvent(event, userId));
                }
                default -> false;
            };
        }

        return false;
    }
    

    private boolean canManageEvent(Event event, @NonNull Long userId) {
        // SuperAdmin can manage all events
        if (hasPermission(userId, "event.manage.all")) {
            return true;
        }

        // Admin can only manage their own events (not other admins' events)
        if (hasPermission(userId, "event.manage.own")) {
            return event.getOrganizer() != null && Objects.equals(event.getOrganizer().getId(), userId);
        }

        return false;
    }
    
    private boolean isUserInvitedToEvent(Event event, @NonNull Long userId) {
        if (event.getAttendees() == null) {
            return false;
        }
        
        return event.getAttendees().stream()
                .anyMatch(attendee -> Objects.equals(attendee.getId(), userId));
    }
    
    @Transactional(readOnly = true)
    public List<EventResponseDTO> getEventsForUser(@NonNull Long userId) {
        // Get all non-deleted events
        List<Event> allEvents = eventRepository.findAllByDeletedFalse();
        
        // Filter events based on user permissions and role
        return allEvents.stream()
                .filter(event -> canViewEvent(event, userId))
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public boolean attendEvent(@NonNull Long eventId, @NonNull Long userId) {
        // Check if user has permission to attend events
        if (!hasPermission(userId, "event.attend")) {
            throw new RuntimeException("You don't have permission to attend events");
        }
        
        // Get event
        Optional<Event> eventOpt = eventRepository.findById(eventId);
        
        if (eventOpt.isPresent()) {
            Event event = eventOpt.get();
            
            // Check if user can view this event
            if (!canViewEvent(event, userId)) {
                throw new RuntimeException("You cannot attend this event");
            }
            
            // Add user to attendees if not already there
            if (event.getAttendees() == null) {
                event.setAttendees(new java.util.HashSet<>());
            }
            
            // Check if user is already in attendees
            boolean isAlreadyAttending = event.getAttendees().stream()
                    .anyMatch(attendee -> Objects.equals(attendee.getId(), userId));
                    
            if (!isAlreadyAttending) {
                // Get user to add to attendees
                userRepository.findById(userId).ifPresent(user -> {
                    event.getAttendees().add(user);
                    event.recordUpdate("system");
                    eventRepository.save(event);
                });
                return true;
            }
        }
        
        return false;
    }

    /**
     * Allow a user to attend a PUBLIC event (self-registration).
     * 
     * Purpose:
     * - Users can register themselves for PUBLIC events
     * - PRIVATE and INVITE_ONLY events are blocked (require invitation)
     * 
     * Flow:
     * 1. Check user has "event.attend" permission
     * 2. Fetch event from database
     * 3. Validate event visibility is PUBLIC
     * 4. Check event hasn't started yet
     * 5. Check user isn't already attending
     * 6. Add user to attendees set
     * 7. Save to database
     * 
     * Business Rules:
     * - Only PUBLIC events allow self-registration
     * - PRIVATE events: throw exception (need invitation)
     * - INVITE_ONLY events: throw exception (need invitation)
     * - Cannot attend past events
     * - Cannot register twice
     * 
     * @param eventId The ID of the event to attend
     * @param userId The ID of the user attending
     * @return true if successfully registered, false if already attending
     * @throws RuntimeException if not PUBLIC event or other validation fails
     */
    @Transactional
    public boolean attendPublicEvent(@NonNull Long eventId, @NonNull Long userId) {
        logger.debugWithContext(
            "EventService",
            "attendPublicEvent",
            "User attempting to attend PUBLIC event: eventId={}, userId={}",
            eventId, userId
        );
        
        // ========== STEP 1: Check Permission ==========
        if (!hasPermission(userId, "event.attend")) {
            logger.warnWithContext(
                "EventService",
                "attendPublicEvent",
                "User lacks permission to attend events: userId={}",
                userId
            );
            throw new RuntimeException("You don't have permission to attend events");
        }
        
        // ========== STEP 2: Fetch Event ==========
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> {
                logger.warnWithContext(
                    "EventService",
                    "attendPublicEvent",
                    "Event not found: eventId={}",
                    eventId
                );
                return new RuntimeException("Event not found with ID: " + eventId);
            });
        
        logger.debugWithContext(
            "EventService",
            "attendPublicEvent",
            "Event found: eventId={}, title='{}', visibility={}",
            eventId, event.getTitle(), event.getVisibility()
        );
        
        // ========== STEP 3: Check Visibility - MUST BE PUBLIC ==========
        if (event.getVisibility() != Event.Visibility.PUBLIC) {
            logger.warnWithContext(
                "EventService",
                "attendPublicEvent",
                "Cannot self-register for non-PUBLIC event: eventId={}, visibility={}",
                eventId, event.getVisibility()
            );
            throw new RuntimeException(
                "This event is " + event.getVisibility().getDisplayName() + 
                ". Only PUBLIC events allow self-registration. " +
                "Please contact the organizer for an invitation."
            );
        }
        
        // ========== STEP 4: Check Event Time ==========
        if (event.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            logger.warnWithContext(
                "EventService",
                "attendPublicEvent",
                "Cannot attend past event: eventId={}, startTime={}",
                eventId, event.getStartTime()
            );
            throw new RuntimeException("Cannot attend an event that has already started");
        }
        
        // ========== STEP 5: Initialize Attendees Set ==========
        if (event.getAttendees() == null) {
            event.setAttendees(new java.util.HashSet<>());
        }
        
        // ========== STEP 6: Check if Already Attending ==========
        boolean isAlreadyAttending = event.getAttendees().stream()
                .anyMatch(attendee -> Objects.equals(attendee.getId(), userId));
        
        if (isAlreadyAttending) {
            logger.debugWithContext(
                "EventService",
                "attendPublicEvent",
                "User already attending: eventId={}, userId={}",
                eventId, userId
            );
            return false;  // Already attending, no change needed
        }
        
        // ========== STEP 7: Add User to Attendees ==========
        userRepository.findById(userId).ifPresentOrElse(
            user -> {
                event.getAttendees().add(user);
                event.recordUpdate("system");
                eventRepository.save(event);
                
                logger.infoWithContext(
                    "EventService",
                    "attendPublicEvent",
                    "User successfully registered for PUBLIC event: eventId={}, userId={}, attendeeCount={}",
                    eventId, userId, event.getAttendees().size()
                );
            },
            () -> {
                logger.warnWithContext(
                    "EventService",
                    "attendPublicEvent",
                    "User not found: userId={}",
                    userId
                );
                throw new RuntimeException("User not found with ID: " + userId);
            }
        );
        
        return true;
    }

    /**
     * Invite a user to a PRIVATE or INVITE_ONLY event (organizer action).
     * 
     * Purpose:
     * - Event organizer can invite users to their private events
     * - Only organizer has this privilege
     * - PUBLIC events don't need invitations (users self-register)
     * 
     * Flow:
     * 1. Fetch event from database
     * 2. Verify caller is the event organizer
     * 3. Validate event visibility is PRIVATE or INVITE_ONLY
     * 4. Check invited user exists
     * 5. Check user isn't already attending
     * 6. Add user to attendees set
     * 7. Save to database
     * 
     * Business Rules:
     * - Only event organizer can invite
     * - Can invite to PRIVATE or INVITE_ONLY events
     * - PUBLIC events: throw exception (users can self-register)
     * - Cannot invite to past events
     * - Cannot invite same user twice
     * 
     * @param eventId The ID of the event
     * @param invitedUserId The ID of the user to invite
     * @param organizerId The ID of the organizer (caller)
     * @return true if successfully invited, false if already attending
     * @throws RuntimeException if not organizer or other validation fails
     */
    @Transactional
    public boolean inviteToPrivateEvent(
            @NonNull Long eventId, 
            @NonNull Long invitedUserId, 
            @NonNull Long organizerId) {
        
        logger.debugWithContext(
            "EventService",
            "inviteToPrivateEvent",
            "Organizer attempting to invite user: eventId={}, invitedUserId={}, organizerId={}",
            eventId, invitedUserId, organizerId
        );
        
        // ========== STEP 1: Fetch Event ==========
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> {
                logger.warnWithContext(
                    "EventService",
                    "inviteToPrivateEvent",
                    "Event not found: eventId={}",
                    eventId
                );
                return new RuntimeException("Event not found with ID: " + eventId);
            });
        
        logger.debugWithContext(
            "EventService",
            "inviteToPrivateEvent",
            "Event found: eventId={}, title='{}', visibility={}, actualOrganizerId={}",
            eventId, event.getTitle(), event.getVisibility(), event.getOrganizer().getId()
        );
        
        // ========== STEP 2: Verify Caller is Organizer ==========
        if (!Objects.equals(event.getOrganizer().getId(), organizerId)) {
            logger.warnWithContext(
                "EventService",
                "inviteToPrivateEvent",
                "User is not the event organizer: eventId={}, attemptedBy={}, actualOrganizer={}",
                eventId, organizerId, event.getOrganizer().getId()
            );
            throw new RuntimeException(
                "Only the event organizer can invite users to this event"
            );
        }
        
        // ========== STEP 3: Check Visibility - Must NOT be PUBLIC ==========
        if (event.getVisibility() == Event.Visibility.PUBLIC) {
            logger.warnWithContext(
                "EventService",
                "inviteToPrivateEvent",
                "Cannot invite to PUBLIC event (users can self-register): eventId={}",
                eventId
            );
            throw new RuntimeException(
                "This is a PUBLIC event. Users can register themselves without invitation."
            );
        }
        
        // ========== STEP 4: Check Event Time ==========
        if (event.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            logger.warnWithContext(
                "EventService",
                "inviteToPrivateEvent",
                "Cannot invite to past event: eventId={}, startTime={}",
                eventId, event.getStartTime()
            );
            throw new RuntimeException("Cannot invite to an event that has already started");
        }
        
        // ========== STEP 5: Verify Invited User Exists ==========
        userRepository.findById(invitedUserId)
            .orElseThrow(() -> {
                logger.warnWithContext(
                    "EventService",
                    "inviteToPrivateEvent",
                    "Invited user not found: userId={}",
                    invitedUserId
                );
                return new RuntimeException("User not found with ID: " + invitedUserId);
            });
        
        // ========== STEP 6: Initialize Attendees Set ==========
        if (event.getAttendees() == null) {
            event.setAttendees(new java.util.HashSet<>());
        }
        
        // ========== STEP 7: Check if Already Attending ==========
        boolean isAlreadyAttending = event.getAttendees().stream()
                .anyMatch(attendee -> Objects.equals(attendee.getId(), invitedUserId));
        
        if (isAlreadyAttending) {
            logger.debugWithContext(
                "EventService",
                "inviteToPrivateEvent",
                "User already attending: eventId={}, userId={}",
                eventId, invitedUserId
            );
            return false;  // Already attending, no change needed
        }
        
        // ========== STEP 8: Add User to Attendees ==========
        userRepository.findById(invitedUserId).ifPresent(user -> {
            event.getAttendees().add(user);
            event.recordUpdate("system");
            eventRepository.save(event);
            
            logger.infoWithContext(
                "EventService",
                "inviteToPrivateEvent",
                "User successfully invited to {} event: eventId={}, invitedUserId={}, organizerId={}, attendeeCount={}",
                event.getVisibility().getDisplayName(),
                eventId, invitedUserId, organizerId, event.getAttendees().size()
            );
        });
        
        return true;
    }
}
