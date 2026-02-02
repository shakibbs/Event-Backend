
    package com.event_management_system.service;

    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Objects;
    import java.util.Optional;
    import java.util.stream.Collectors;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.jdbc.core.JdbcTemplate;
    import org.springframework.lang.NonNull;
    import org.springframework.scheduling.annotation.Async;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    import com.event_management_system.dto.EventRequestDTO;
    import com.event_management_system.dto.EventResponseDTO;
    import com.event_management_system.entity.Event;
    import com.event_management_system.entity.EventAttendees;
    import com.event_management_system.entity.User;
    import com.event_management_system.exception.GlobalExceptionHandler.BadRequestException;
    import com.event_management_system.exception.GlobalExceptionHandler.ForbiddenException;
    import com.event_management_system.exception.GlobalExceptionHandler.ResourceNotFoundException;
    import com.event_management_system.mapper.EventMapper;
    import com.event_management_system.repository.EventAttendeesRepository;
    import com.event_management_system.repository.EventRepository;
    import com.event_management_system.repository.UserRepository;

@Service
public class EventService {

    @Autowired
    private ApplicationLoggerService log;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventAttendeesRepository eventAttendeesRepository;

    @Autowired
    private EventMapper eventMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityHistoryService activityHistoryService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private org.springframework.core.task.TaskExecutor taskExecutor;

        
        @Transactional(readOnly = true)
        public java.util.List<EventAttendees> getAttendeesForEvent(@NonNull Long eventId, @NonNull Long currentUserId) {
            Event event = getEventEntityById(eventId, currentUserId);
            return eventAttendeesRepository.findByEvent(event);
        }

        @Transactional(readOnly = true)
        public Event getEventEntityById(@NonNull Long eventId, @NonNull Long currentUserId) {
            Event event = eventRepository.findById(eventId)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
            if (!canViewEvent(event, currentUserId)) {
                throw new RuntimeException("You don't have permission to view this event");
            }
            return event;
        }

    @Transactional
    public EventResponseDTO createEvent(@NonNull EventRequestDTO eventRequestDTO, @NonNull Long currentUserId) {
        log.trace("[EventService] TRACE - createEvent() called with userId=" + currentUserId + ", title="
                + eventRequestDTO.getTitle());

        log.debug("[EventService] DEBUG - createEvent() - Checking permissions for user " + currentUserId);
        boolean hasManageOwn = hasPermission(currentUserId, "event.manage.own");
        boolean hasManageAll = hasPermission(currentUserId, "event.manage.all");

        log.debug("[EventService] DEBUG - createEvent() - hasPermission('event.manage.own'): " + hasManageOwn);
        log.debug("[EventService] DEBUG - createEvent() - hasPermission('event.manage.all'): " + hasManageAll);

        if (!hasManageOwn && !hasManageAll) {
            log.warn("[EventService] WARN - User " + currentUserId + " not authorized to create events");
            throw new RuntimeException("You don't have permission to create events");
        }

        if (!eventRequestDTO.isDateRangeValid()) {
            log.warn("[EventService] WARN - Invalid date range: end time must be after start time");
            throw new IllegalArgumentException("End time must be after start time");
        }

        if (!eventRequestDTO.areDatesInFuture()) {
            log.warn("[EventService] WARN - Invalid dates: start and end times must be in the future");
            throw new IllegalArgumentException("Start time and end time must be in the future");
        }

        Event event = eventMapper.toEntity(eventRequestDTO);

        User user = userRepository.findById(currentUserId)
            .orElseThrow(() -> new RuntimeException("User not found for event creation: id=" + currentUserId));
        event.setOrganizer(user);
        // Set createdBy to the user's email for audit and filtering
        event.recordCreation(user.getEmail());
        log.debug("[EventService] DEBUG - createEvent() - Set organizer and createdBy to user " + user.getId() + ", email: " + user.getEmail());
        Event savedEvent = eventRepository.save(event);

        log.info("[EventService] INFO - Event created successfully: eventId=" + savedEvent.getId() + ", title="
                + savedEvent.getTitle() + ", userId=" + currentUserId);

        try {
            activityHistoryService.recordActivity(
                Objects.requireNonNull(user, "User should not be null"),
                com.event_management_system.entity.UserActivityHistory.ActivityType.EVENT_CREATED,
                "Event: " + savedEvent.getTitle(),
                "0.0.0.0",
                "system",
                java.util.UUID.randomUUID().toString());
        } catch (Exception e) {
            log.error("[EventService] ERROR - Failed to record event creation activity: " + e.getMessage());
        }

        return eventMapper.toDto(savedEvent);
    }

   
    @Transactional(readOnly = true)
    public List<EventResponseDTO> findPublicUpcomingEvents() {
        List<Event> events = eventRepository.findByVisibilityAndEventStatusAndDeletedFalse(
            Event.Visibility.PUBLIC, Event.EventStatus.UPCOMING
        );
        return events.stream().map(eventMapper::toDto).collect(Collectors.toList());
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
    public Optional<EventResponseDTO> updateEvent(@NonNull Long id, @NonNull EventRequestDTO eventRequestDTO,
            @NonNull Long currentUserId) {

        log.trace("[EventService] TRACE - updateEvent() called with eventId=" + id + ", userId=" + currentUserId);

        log.debug("[EventService] DEBUG - updateEvent() - Checking manage permission for event " + id);
        return eventRepository.findById(id).map(existingEvent -> {
            if (!canManageEvent(existingEvent, currentUserId)) {
                log.warn("[EventService] WARN - User " + currentUserId + " not authorized to update event " + id);
                throw new RuntimeException("You don't have permission to update this event");
            }

            if (!eventRequestDTO.isDateRangeValid()) {
                log.warn("[EventService] WARN - Invalid date range in update: end time must be after start time");
                throw new IllegalArgumentException("End time must be after start time");
            }

            if (!eventRequestDTO.areDatesInFuture()) {
                log.warn(
                        "[EventService] WARN - Invalid future dates in update: start and end times must be in the future");
                throw new IllegalArgumentException("Start time and end time must be in the future");
            }

            log.debug("[EventService] DEBUG - updateEvent() - Updating event entity with new data");
            eventMapper.updateEntity(eventRequestDTO, existingEvent);

            existingEvent.recordUpdate("system");
            Event updatedEvent = eventRepository.save(existingEvent);

            log.info("[EventService] INFO - Event updated successfully: eventId=" + updatedEvent.getId() + ", title="
                    + updatedEvent.getTitle() + ", userId=" + currentUserId);

            userRepository.findById(currentUserId).ifPresent(user -> {
                try {
                    activityHistoryService.recordActivity(
                            Objects.requireNonNull(user, "User should not be null"),
                            com.event_management_system.entity.UserActivityHistory.ActivityType.EVENT_UPDATED,
                            "Event: " + updatedEvent.getTitle(),
                            "0.0.0.0",
                            "system",
                            java.util.UUID.randomUUID().toString());
                } catch (Exception e) {
                    log.error("[EventService] ERROR - Failed to record event update activity: " + e.getMessage());
                }
            });

            return eventMapper.toDto(updatedEvent);
        });
    }

    @Transactional
    public boolean deleteEvent(@NonNull Long id, @NonNull Long currentUserId) {
        log.trace("[EventService] TRACE - deleteEvent() called with eventId=" + id + ", userId=" + currentUserId);

        return eventRepository.findById(id).map(event -> {
            log.debug("[EventService] DEBUG - deleteEvent() - Checking manage permission for event " + id);
            if (!canManageEvent(event, currentUserId)) {
                log.warn("[EventService] WARN - User " + currentUserId + " not authorized to delete event " + id);
                throw new RuntimeException("You don't have permission to delete this event");
            }

            String eventTitle = event.getTitle();
            log.debug("[EventService] DEBUG - deleteEvent() - Marking event as deleted: " + eventTitle);
            event.markDeleted();
            eventRepository.save(event);

            log.info("[EventService] INFO - Event deleted successfully: eventId=" + event.getId() + ", title="
                    + eventTitle + ", userId=" + currentUserId);

            userRepository.findById(currentUserId).ifPresent(user -> {
                try {
                    activityHistoryService.recordActivity(
                            Objects.requireNonNull(user, "User should not be null"),
                            com.event_management_system.entity.UserActivityHistory.ActivityType.EVENT_DELETED,
                            "Event: " + eventTitle + " (ID: " + event.getId() + ")",
                            "0.0.0.0",
                            "system",
                            java.util.UUID.randomUUID().toString());
                } catch (Exception e) {
                    log.error("[EventService] ERROR - Failed to record event deletion activity: " + e.getMessage());
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

    private boolean hasPermission(@NonNull Long userId, String permissionName) {
        return userService.hasPermission(userId, permissionName);
    }

    private boolean canViewEvent(Event event, @NonNull Long userId) {
        if (hasPermission(userId, "event.manage.all")) {
            return true;
        }

        if (hasPermission(userId, "event.view.all")) {
            return true;
        }

        if (event.getOrganizer() != null && Objects.equals(event.getOrganizer().getId(), userId)) {
            return true;
        }

        if (event.getVisibility() != null) {
            if (event.getVisibility() == Event.Visibility.PUBLIC) {
                return hasPermission(userId, "event.view.public");
            } else if (event.getVisibility() == Event.Visibility.PRIVATE) {
                var user = userRepository.findById(userId).orElse(null);
                return hasPermission(userId, "event.view.invited") &&
                        (user != null && isUserInvitedToEvent(event, userId));
            }
        }

        return false;
    }

    private boolean canManageEvent(Event event, @NonNull Long userId) {
        if (hasPermission(userId, "event.manage.all")) {
            return true;
        }

        if (hasPermission(userId, "event.manage.own")) {
            return event.getOrganizer() != null && Objects.equals(event.getOrganizer().getId(), userId);
        }

        return false;
    }

    private boolean isUserInvitedToEvent(Event event, @NonNull Long userId) {
        var user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }

        return eventAttendeesRepository.findByUser(user).stream()
                .anyMatch(ea -> Objects.equals(ea.getEvent().getId(), event.getId()));
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> getEventsForUser(@NonNull Long userId) {
        List<Event> allEvents = eventRepository.findAllByDeletedFalse();

        return allEvents.stream()
                .filter(event -> canViewEvent(event, userId))
                .map(eventMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean attendEvent(@NonNull Long eventId, @NonNull Long userId) {
        if (!hasPermission(userId, "event.attend")) {
            throw new RuntimeException("You don't have permission to attend events");
        }

        Optional<Event> eventOpt = eventRepository.findById(eventId);

        if (eventOpt.isPresent()) {
            Event event = eventOpt.get();

            if (!canViewEvent(event, userId)) {
                throw new RuntimeException("You cannot attend this event");
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

            boolean isAlreadyAttending = eventAttendeesRepository.existsByEventAndEmail(event, user.getEmail());

            if (!isAlreadyAttending) {
                EventAttendees attendee = EventAttendees.builder()
                        .event(event)
                        .user(user)
                        .email(user.getEmail())
                        .invitationStatus(EventAttendees.InvitationStatus.ACCEPTED)
                        .invitationSentAt(LocalDateTime.now())
                        .responseAt(LocalDateTime.now())
                        .build();

                attendee.recordCreation("system");
                eventAttendeesRepository.save(attendee);
                return true;
            }
        }

        return false;
    }

    @Transactional
    public boolean attendPublicEvent(@NonNull Long eventId, @NonNull Long userId) {
        log.debug("attendPublicEvent",
                "User attempting to attend PUBLIC event: eventId={}, userId={}",
                eventId, userId);

        if (!hasPermission(userId, "event.attend")) {
            log.warn("attendPublicEvent",
                    "User lacks permission to attend events: userId={}",
                    userId);
            throw new RuntimeException("You don't have permission to attend events");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("attendPublicEvent",
                            "Event not found: eventId={}",
                            eventId);
                    return new RuntimeException("Event not found with ID: " + eventId);
                });

        log.debug("attendPublicEvent",
                "Event found: eventId={}, title='{}', visibility={}",
                eventId, event.getTitle(), event.getVisibility());

        if (event.getVisibility() != Event.Visibility.PUBLIC) {
            log.warn("attendPublicEvent",
                    "Cannot self-register for non-PUBLIC event: eventId={}, visibility={}",
                    eventId, event.getVisibility());
            throw new RuntimeException(
                    "This event is " + event.getVisibility().getDisplayName() +
                            ". Only PUBLIC events allow self-registration. " +
                            "Please contact the organizer for an invitation.");
        }

        if (event.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            log.warn("attendPublicEvent",
                    "Cannot attend past event: eventId={}, startTime={}",
                    eventId, event.getStartTime());
            throw new RuntimeException("Cannot attend an event that has already started");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        boolean isAlreadyAttending = eventAttendeesRepository.existsByEventAndEmail(event, user.getEmail());

        if (isAlreadyAttending) {
            log.debug("attendPublicEvent",
                    "User already attending: eventId={}, userId={}",
                    eventId, userId);
            return false;
        }

        EventAttendees attendee = EventAttendees.builder()
                .event(event)
                .user(user)
                .email(user.getEmail())
                .invitationStatus(EventAttendees.InvitationStatus.ACCEPTED)
                .invitationSentAt(LocalDateTime.now())
                .responseAt(LocalDateTime.now())
                .build();

        attendee.recordCreation("system");
        eventAttendeesRepository.save(attendee);

        log.info("attendPublicEvent",
                "User successfully registered for PUBLIC event: eventId={}, userId={}",
                eventId, userId);

        return true;
    }

    
    @Transactional
    public java.util.Map<String, Object> sendBulkInvitations(
            @NonNull Long eventId,
            org.springframework.web.multipart.MultipartFile file,
            @NonNull Long organizerId) {

        log.info("[EventService] INFO - sendBulkInvitations() initiated for eventId={}, organizerId={}", eventId, organizerId);

        Event event = validateEvent(eventId, organizerId);

        // Load CSV invitations (external emails)
        java.util.concurrent.CompletableFuture<java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO>> csvFuture =
            java.util.concurrent.CompletableFuture.supplyAsync(
                () -> loadExternalInvitesDirectFromCsv(file),
                (java.util.concurrent.Executor) taskExecutor
            );

        // Load temp_email invitations (pending emails from temp table)
        java.util.concurrent.CompletableFuture<java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO>> tempFuture =
            java.util.concurrent.CompletableFuture.supplyAsync(
                () -> {
                    java.util.List<String> tempEmails = fetchPendingEmails();
                    return tempEmails.stream()
                        .map(email -> new com.event_management_system.dto.InviteAttendeeRequestDTO(null, email, ""))
                        .collect(java.util.stream.Collectors.toList());
                },
                (java.util.concurrent.Executor) taskExecutor
            );

        // Load registered users (excluding organizer)
        java.util.concurrent.CompletableFuture<java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO>> registeredFuture =
            java.util.concurrent.CompletableFuture.supplyAsync(
                () -> loadRegisteredUsers(organizerId),
                (java.util.concurrent.Executor) taskExecutor
            );

        java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO> csvInvites = csvFuture.join();
        java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO> tempInvites = tempFuture.join();
        java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO> registeredInvites = registeredFuture.join();

        int totalCsv = csvInvites.size();
        int totalTemp = tempInvites.size();
        int totalRegistered = registeredInvites.size();

        if (totalCsv == 0 && totalTemp == 0 && totalRegistered == 0) {
            log.warn("[EventService] WARN - No emails to invite for eventId={}", eventId);
            throw new BadRequestException("No emails to invite");
        }

        if (totalCsv > 0) {
            log.info("[EventService] INFO - Starting CSV invitations for eventId={}, totalCsv={}", eventId, totalCsv);
            processBulkInvitationsAsync(event, csvInvites);
        }
        if (totalTemp > 0) {
            log.info("[EventService] INFO - Starting temp_email invitations for eventId={}, totalTemp={}", eventId, totalTemp);
            processBulkInvitationsAsync(event, tempInvites);
        }
        if (totalRegistered > 0) {
            log.info("[EventService] INFO - Starting registered user invitations for eventId={}, totalRegistered={}", eventId, totalRegistered);
            processBulkInvitationsAsync(event, registeredInvites);
        }

        return java.util.Map.of(
                "status", "processing",
                "message", "CSV, temp_email, and registered user invitations submitted for processing",
                "csvTotal", totalCsv,
                "tempTotal", totalTemp,
                "registeredTotal", totalRegistered,
                "eventId", eventId
        );
    }
    

    private Event validateEvent(Long eventId, Long organizerId) {
        log.debug("[EventService] DEBUG - validateEvent() for eventId={}, organizerId={}", eventId, organizerId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("[EventService] WARN - Event not found: eventId={}", eventId);
                    return new ResourceNotFoundException("Event not found with ID: " + eventId);
                });

        if (!Objects.equals(event.getOrganizer().getId(), organizerId)
                && !hasPermission(organizerId, "event.manage.all")) {
            log.warn("[EventService] WARN - Unauthorized invitation attempt for eventId={}, organizerId={}", eventId, organizerId);
            throw new ForbiddenException("Only the event organizer can invite users");
        }

        if (event.getStartTime().isBefore(java.time.LocalDateTime.now())) {
            log.warn("[EventService] WARN - Cannot invite to past event: eventId={}, startTime={}", eventId, event.getStartTime());
            throw new BadRequestException("Cannot invite to a past event");
        }

        log.debug("[EventService] DEBUG - Event validation passed: eventId={}", eventId);
        return event;
    }


    
    private java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO> loadRegisteredUsers(Long organizerId) {
        log.debug("[EventService] DEBUG - loadRegisteredUsers() started");

        java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO> users = userRepository.findAll().stream()
                .filter(u -> !u.getId().equals(organizerId))
                .map(u -> new com.event_management_system.dto.InviteAttendeeRequestDTO(
                        u.getId(), u.getEmail(), u.getFullName()))
                .collect(java.util.stream.Collectors.toList());

        log.info("[EventService] INFO - Loaded {} registered users for invitations", users.size());
        return users;
    }

    public java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO> loadExternalInvitesDirectFromCsv(org.springframework.web.multipart.MultipartFile file) {
        log.debug("[EventService] DEBUG - loadExternalInvitesDirectFromCsv() started");

        java.util.List<String> emails = readCsvEmails(file);
        if (emails.isEmpty()) {
            log.info("[EventService] INFO - No valid emails found in CSV file");
            return java.util.Collections.emptyList();
        }
        log.info("[EventService] INFO - Read {} emails from CSV file (direct flow)", emails.size());
        java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO> externalInvites = emails.stream()
                .map(email -> new com.event_management_system.dto.InviteAttendeeRequestDTO(null, email, ""))
                .collect(java.util.stream.Collectors.toList());
        log.info("[EventService] INFO - Prepared {} external invites directly from CSV (bypassing temp_email)", externalInvites.size());
        return externalInvites;
    }

    private java.util.List<String> readCsvEmails(org.springframework.web.multipart.MultipartFile file) {
        log.debug("[EventService] DEBUG - readCsvEmails() started");

        if (file == null || file.isEmpty()) {
            log.debug("[EventService] DEBUG - No CSV file provided");
            return new java.util.ArrayList<>();
        }

        try (java.io.BufferedReader br = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream()))) {

            java.util.List<String> lines = br.lines()
                .filter(line -> !line.trim().isEmpty())
                .collect(java.util.stream.Collectors.toList());

            java.util.List<String> emails = new java.util.ArrayList<>();
            boolean headerLooksLikeHeader = false;
            if (!lines.isEmpty()) {
                String first = lines.get(0).toLowerCase();
                headerLooksLikeHeader = first.contains("email") && !first.contains("@");
            }
            for (int i = (headerLooksLikeHeader ? 1 : 0); i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                if (parts.length > 0) {
                    String email = parts[0].trim().toLowerCase();
                    if (email.contains("@")) {
                        emails.add(email);
                    }
                }
            }
            log.debug("[EventService] DEBUG - Parsed {} valid emails from CSV", emails.size());
            return emails;

        } catch (java.io.IOException e) {
            log.error("[EventService] ERROR - Failed to read CSV file: {}", e.getMessage());
            throw new RuntimeException("Failed to process CSV file", e);
        }
    }
    

   
    @Async("taskExecutor")

    public void processBulkInvitationsAsync(
            Event event,
            java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO> invitations) {

        log.info("[EventService] INFO - processBulkInvitationsAsync() started for eventId={}, totalInvitations={}",
                event.getId(), invitations.size());



        try {
            long start = System.currentTimeMillis();
            int batchSize = 10;
            int total = invitations.size();
            int sent = 0;
            long successCount = 0;
            long failureCount = 0;
            while (sent < total) {
                int end = Math.min(sent + batchSize, total);
                java.util.List<com.event_management_system.dto.InviteAttendeeRequestDTO> batch = invitations.subList(sent, end);
                java.util.List<java.util.concurrent.CompletableFuture<Boolean>> tasks = new java.util.ArrayList<>();
                for (com.event_management_system.dto.InviteAttendeeRequestDTO invite : batch) {
                    try {
                        tasks.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                            long emailStart = System.currentTimeMillis();
                            boolean result = processInvitation(event, invite);
                            long emailEnd = System.currentTimeMillis();
                            log.info("[EventService] INFO - Email send time for {}: {} ms", invite.getEmail(), (emailEnd - emailStart));
                            return result;
                        }, (java.util.concurrent.Executor) taskExecutor));
                    } catch (java.util.concurrent.RejectedExecutionException ex) {
                        log.error("[EventService] ERROR - TaskExecutor saturated! Could not submit invitation for email: {}. Consider increasing pool size or queue capacity.", invite.getEmail());
                    }
                }
                java.util.concurrent.CompletableFuture<Boolean>[] futuresArray = tasks.toArray(new java.util.concurrent.CompletableFuture[0]);
                java.util.concurrent.CompletableFuture.allOf(futuresArray).join();
                for (java.util.concurrent.CompletableFuture<Boolean> task : tasks) {
                    try {
                        if (task.join()) successCount++;
                        else failureCount++;
                    } catch (Exception e) {
                        log.debug("[EventService] DEBUG - Task failed: {}", e.getMessage());
                        failureCount++;
                    }
                }
                sent = end;
                if (sent < total) {
                    log.info("[EventService] INFO - Throttling: sleeping 2 seconds between batches to avoid Gmail blocking");
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }
            log.info("[EventService] INFO - processBulkInvitationsAsync() completed for eventId={}, successful={}, failed={}",
                    event.getId(), successCount, failureCount);
            log.debug("[EventService] DEBUG - All batches completed in {} ms", (System.currentTimeMillis() - start));
        } catch (Exception e) {
            log.error("[EventService] ERROR - Exception in processBulkInvitationsAsync(): {}", e.getMessage());
            log.error("[EventService] ERROR - Stack trace: ", e);
        }
    }
    

   
    private boolean processInvitation(Event event, com.event_management_system.dto.InviteAttendeeRequestDTO invite) {
        log.debug("[EventService] DEBUG - processInvitation() started for email: {}", invite.getEmail());

        try {
            if (!invite.isValid()) {
                log.warn("[EventService] WARN - Invalid invitation data: {}", invite);
                return false;
            }

            String email = invite.isRegisteredUser()
                    ? userRepository.findById(invite.getUserId())
                            .orElseThrow(() -> new RuntimeException("User not found"))
                            .getEmail()
                    : invite.getEmail();

            if (eventAttendeesRepository.existsByEventAndEmail(event, email)) {
                log.debug("[EventService] DEBUG - Email already invited, skipping: {}", email);
                return false;
            }

            User user = invite.isRegisteredUser()
                    ? userRepository.findById(invite.getUserId()).orElse(null)
                    : userRepository.findByEmail(email).orElse(null);

            EventAttendees attendee = EventAttendees.builder()
                    .event(event)
                    .user(user)
                    .email(email)
                    .invitationStatus(EventAttendees.InvitationStatus.PENDING)
                    .invitationSentAt(java.time.LocalDateTime.now())
                    .build();

            attendee.recordCreation("system");  // Audit trail
            EventAttendees savedAttendee = eventAttendeesRepository.save(attendee);

            log.debug("[EventService] DEBUG - Created EventAttendees for email: {}", email);

            boolean emailSent = emailService.sendWithRetry(
                    () -> emailService.sendInvitationEmail(event, email, savedAttendee.getInvitationToken()),
                    email,
                    3
            );

            if (emailSent) {
                log.info("[EventService] INFO - Successfully sent invitation to: {}", email);
                return true;
            } else {
                log.warn("[EventService] WARN - Failed to send invitation to: {} after 3 retry attempts", email);
                return false;
            }

        } catch (Exception e) {
            log.error("[EventService] ERROR - Exception processing invitation for email {}: {}", invite.getEmail(), e.getMessage());
            return false;
        }
    }
    

    @Transactional
    public EventResponseDTO processEventAction(@NonNull Long eventId,
            @NonNull com.event_management_system.dto.EventActionRequestDTO actionRequest,
            @NonNull Long checkerId) {
        log.trace("[EventService] TRACE - processEventAction() called with eventId={}, checkerId={}, action={}",
                eventId, checkerId, actionRequest.getAction());

        if (!hasPermission(checkerId, "event.approve")) {
            log.warn("[EventService] WARN - User lacks permission to approve/reject events: userId={}",
                    checkerId);
            throw new ForbiddenException("You don't have permission to approve or reject events");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with ID: " + eventId));

        if (Objects.equals(event.getOrganizer().getId(), checkerId)) {
            if (!userService.hasRole(checkerId, "SuperAdmin")) {
                log.warn(
                        "[EventService] WARN - Event creator cannot approve/reject own event (not SuperAdmin): eventId={}, organizerId={}",
                        eventId, checkerId);
                throw new BadRequestException("You cannot approve or reject your own event. Another admin must do it.");
            }
        }

        if (event.getApprovalStatus() != Event.ApprovalStatus.PENDING) {
            log.warn("[EventService] WARN - Event not in PENDING status: eventId={}, currentStatus={}",
                    eventId, event.getApprovalStatus());
            throw new BadRequestException("Event is already " + event.getApprovalStatus().getDisplayName());
        }

        if (actionRequest.getAction() == null) {
            throw new BadRequestException("Action (APPROVE/REJECT) is required");
        }

        User checker = userRepository.findById(checkerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + checkerId));

        log.debug("[EventService] DEBUG - Processing event action: action={}", actionRequest.getAction());

        if (actionRequest.getAction() == com.event_management_system.dto.EventActionRequestDTO.Action.REJECT) {
            if (actionRequest.getRemarks() == null || actionRequest.getRemarks().trim().isEmpty()) {
                log.warn("[EventService] WARN - Remarks missing for event rejection: eventId={}", eventId);
                throw new BadRequestException("Remarks are mandatory when rejecting an event");
            }
            event.setApprovalStatus(Event.ApprovalStatus.REJECTED);
            event.setEventStatus(Event.EventStatus.CANCELLED);
            event.setRemarks(actionRequest.getRemarks());
            log.info("[EventService] INFO - Event rejected and cancelled: eventId={}, checkerId={}, remarks={}", eventId, checkerId,
                    actionRequest.getRemarks());
        } else if (actionRequest.getAction() == com.event_management_system.dto.EventActionRequestDTO.Action.APPROVE) {
            event.setApprovalStatus(Event.ApprovalStatus.APPROVED);
            log.info("[EventService] INFO - Event approved: eventId={}, checkerId={}", eventId, checkerId);
        } else {
            log.error("[EventService] ERROR - Unknown action: {}", actionRequest.getAction());
            throw new BadRequestException("Invalid action: " + actionRequest.getAction());
        }

        event.setApprovedBy(checker);
        event.setApprovedAt(LocalDateTime.now());
        event.recordUpdate("system");

        Event savedEvent = eventRepository.save(event);

        log.debug("[EventService] DEBUG - Event action saved successfully: eventId={}, status={}", 
                  eventId, savedEvent.getApprovalStatus());

        return eventMapper.toDto(savedEvent);
    }

    @Transactional
    public void respondToInvitation(@NonNull String invitationToken, @NonNull String action) {
        log.trace("[EventService] TRACE - respondToInvitation() called with token={}, action={}",
                invitationToken, action);

        EventAttendees attendee = eventAttendeesRepository.findByInvitationToken(invitationToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid invitation token"));

        if (attendee.getInvitationStatus() != EventAttendees.InvitationStatus.PENDING) {
            log.warn("[EventService] WARN - Invitation already responded: token={}, currentStatus={}",
                    invitationToken, attendee.getInvitationStatus());
            throw new BadRequestException("You have already responded to this invitation");
        }

        boolean accepted;
        String tempPassword = null;  // Store for credentials email later
        
        if ("ACCEPT".equalsIgnoreCase(action)) {
            attendee.setInvitationStatus(EventAttendees.InvitationStatus.ACCEPTED);
            accepted = true;
            log.info("[EventService] INFO - Invitation accepted: eventId={}, email={}",
                    attendee.getEvent().getId(), attendee.getEmail());

            if (attendee.getUser() == null) {
                try {
                    log.info("[EventService] INFO - Auto-creating account for external user: email={}", 
                             attendee.getEmail());
                    
                    java.util.Map<String, Object> accountInfo = userService.createAutoAccountForInvitee(
                        attendee.getEmail(),
                        attendee.getEmail().split("@")[0] 
                    );
                    
                    User newUser = (User) accountInfo.get("user");
                    tempPassword = (String) accountInfo.get("password");
                    
                    attendee.setUser(newUser);
                    log.info("[EventService] INFO - ✅ Auto account created: email={}, userId={}", 
                             attendee.getEmail(), newUser.getId());
                    
                } catch (Exception e) {
                    log.error("[EventService] ERROR - Failed to auto-create account for invitee: email={}, error={}", 
                             attendee.getEmail(), e.getMessage(), e);
                }
            }

        } else if ("DECLINE".equalsIgnoreCase(action)) {
            attendee.setInvitationStatus(EventAttendees.InvitationStatus.DECLINED);
            accepted = false;
            log.info("[EventService] INFO - Invitation declined: eventId={}, email={}",
                    attendee.getEvent().getId(), attendee.getEmail());
        } else {
            throw new BadRequestException("Invalid action. Must be ACCEPT or DECLINE");
        }

        attendee.setResponseAt(LocalDateTime.now());
        attendee.recordUpdate("system");
        eventAttendeesRepository.save(attendee);

        log.info("[EventService] INFO - Sending invitation response confirmation to: {}", attendee.getEmail());
        emailService.sendInvitationResponseConfirmation(attendee.getEvent(), attendee.getEmail(), accepted);

        if (accepted && attendee.getUser() != null && tempPassword != null) {
            try {
                final String finalEmail = attendee.getEmail();
                final String finalFullName = attendee.getUser().getFullName();
                final String finalPassword = tempPassword;
                final Long finalUserId = attendee.getUser().getId();
                
                log.info("[EventService] INFO - Waiting 1 second before sending credentials email...");
                Thread.sleep(1000);
                
                log.debug("[EventService] DEBUG - Sending credentials email to: {}", finalEmail);
                log.info("[EventService] INFO - Starting credentials email send with retry for: {}", finalEmail);
                
                boolean emailSent = emailService.sendWithRetry(
                    () -> emailService.sendAutoAccountCredentials(
                        finalEmail,
                        finalFullName,
                        finalPassword
                    ),
                    finalEmail,
                    5
                );
                
                if (emailSent) {
                    log.info("[EventService] INFO - ✅ SUCCESS: Credentials email sent after confirmation: email={}, userId={}", 
                             finalEmail, finalUserId);
                } else {
                    log.warn("[EventService] WARN - ⚠️ FAILED: Credentials email send failed after 5 retries: email={}, userId={}", 
                             finalEmail, finalUserId);
                }
            } catch (Exception e) {
                log.error("[EventService] ERROR - Exception sending credentials email: email={}, error={}", 
                         attendee.getEmail(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void holdEvent(Long eventId, Long userId) {

        if (!userService.hasRole(userId, "SuperAdmin")) {
            throw new ForbiddenException("Only SuperAdmin can hold events");
        }

        if (!hasPermission(userId, "event.hold")) {
            throw new ForbiddenException("You don't have permission to hold events");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (!event.isHoldable()) {
            throw new BadRequestException("Can only hold UPCOMING events. Current status: " + event.getEventStatus());
        }

        event.hold();
        event.recordUpdate("superadmin_" + userId);
        eventRepository.save(event);

        log.info("[EventService] INFO - Event held: eventId={}, userId={}", eventId, userId);
    }

    @Transactional
    public void reactivateEvent(Long eventId, Long userId) {

        if (!userService.hasRole(userId, "SuperAdmin")) {
            throw new ForbiddenException("Only SuperAdmin can reactivate events");
        }

        if (!hasPermission(userId, "event.reactivate")) {
            throw new ForbiddenException("You don't have permission to reactivate events");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (!event.isReactivatable()) {
            throw new BadRequestException(
                    "Can only reactivate INACTIVE events. Current status: " + event.getEventStatus());
        }

        event.reactivate();
        event.recordUpdate("superadmin_" + userId);
        eventRepository.save(event);

        log.info("[EventService] INFO - Event reactivated: eventId={}, userId={}", eventId, userId);
    }

    
    public int insertEmailsToTempTable(List<String> emails) {
        log.debug("[EventService] DEBUG - insertEmailsToTempTable() called with {} emails", emails.size());
        
        int inserted = 0;
        for (String email : emails) {
            try {
                jdbcTemplate.update("INSERT INTO temp_email (email) VALUES (?) ON DUPLICATE KEY UPDATE email=email", email.toLowerCase().trim());
                inserted++;
                log.debug("[EventService] DEBUG - Inserted email into temp_email: {}", email);
            } catch (Exception e) {
                log.warn("[EventService] WARN - Failed to insert email into temp_email: {}, error: {}", email, e.getMessage());
            }
        }
        
        log.info("[EventService] INFO - Successfully inserted {} emails into temp_email table", inserted);
        return inserted;
    }

    
    public List<String> fetchPendingEmails() {
        log.debug("[EventService] DEBUG - fetchPendingEmails() called");
        
        try {
            List<String> emails = jdbcTemplate.queryForList(
                "SELECT email FROM temp_email", 
                String.class
            );
            log.debug("[EventService] DEBUG - Fetched {} pending emails from temp_email", emails.size());
            return emails;
        } catch (Exception e) {
            log.error("[EventService] ERROR - Failed to fetch pending emails: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }

   
    public boolean deleteEmailFromTempTable(String email) {
        log.debug("[EventService] DEBUG - deleteEmailFromTempTable() called for: {}", email);
        
        try {
            int rowsAffected = jdbcTemplate.update(
                "DELETE FROM temp_email WHERE email = ?", 
                email.toLowerCase().trim()
            );
            
            if (rowsAffected > 0) {
                log.debug("[EventService] DEBUG - Successfully deleted email from temp_email: {}", email);
                return true;
            } else {
                log.warn("[EventService] WARN - Email not found in temp_email: {}", email);
                return false;
            }
        } catch (Exception e) {
            log.error("[EventService] ERROR - Failed to delete email from temp_email: {}", e.getMessage());
            return false;
        }
    }

}

