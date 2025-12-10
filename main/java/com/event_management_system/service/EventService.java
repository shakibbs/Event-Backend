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
        // Check permission before creating event
        if (!hasPermission(currentUserId, "event.manage.own") &&
            !hasPermission(currentUserId, "event.manage.all")) {
            throw new RuntimeException("You don't have permission to create events");
        }
        
        if (!eventRequestDTO.isDateRangeValid()) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        if (!eventRequestDTO.areDatesInFuture()) {
            throw new IllegalArgumentException("Start time and end time must be in the future");
        }
        
        Event event = eventMapper.toEntity(eventRequestDTO);
        
        // Set organizer
        userRepository.findById(currentUserId).ifPresent(user -> {
            event.setOrganizer(user);
        });
        
        event.recordCreation("system");
        Event savedEvent = eventRepository.save(event);
        
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
                log.error("Failed to record event creation activity: {}", e.getMessage());
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
        // Check if user can manage this event
        return eventRepository.findById(id).map(existingEvent -> {
            if (!canManageEvent(existingEvent, currentUserId)) {
                throw new RuntimeException("You don't have permission to update this event");
            }
            
            if (!eventRequestDTO.isDateRangeValid()) {
                throw new IllegalArgumentException("End time must be after start time");
            }
            
            if (!eventRequestDTO.areDatesInFuture()) {
                throw new IllegalArgumentException("Start time and end time must be in the future");
            }
            
            eventMapper.updateEntity(eventRequestDTO, existingEvent);
           
            // Update audit fields
            existingEvent.recordUpdate("system");
            Event updatedEvent = eventRepository.save(existingEvent);
            
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
                    log.error("Failed to record event update activity: {}", e.getMessage());
                }
            });
            
            return eventMapper.toDto(updatedEvent);
        });
    }

    @Transactional
    public boolean deleteEvent(@NonNull Long id, @NonNull Long currentUserId) {
        return eventRepository.findById(id).map(event -> {
            if (!canManageEvent(event, currentUserId)) {
                throw new RuntimeException("You don't have permission to delete this event");
            }
            
            String eventTitle = event.getTitle();  // Save title before deletion
            event.markDeleted();
            eventRepository.save(event);
            
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
                    log.error("Failed to record event deletion activity: {}", e.getMessage());
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
}
