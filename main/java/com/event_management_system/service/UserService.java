package com.event_management_system.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.event_management_system.dto.UserRequestDTO;
import com.event_management_system.dto.UserResponseDTO;
import com.event_management_system.dto.UserUpdateRequestDTO;
import com.event_management_system.entity.Permission;
import com.event_management_system.entity.Role;
import com.event_management_system.entity.User;
import com.event_management_system.entity.UserActivityHistory;
import com.event_management_system.mapper.UserMapper;
import com.event_management_system.repository.EventRepository;
import com.event_management_system.repository.RoleRepository;
import com.event_management_system.repository.UserRepository;
import com.event_management_system.util.RequestInfoUtil;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class UserService {

        @Autowired
        private UserLoginLogoutHistoryService loginLogoutHistoryService;
    
    @Transactional(readOnly = true)
    public Optional<User> getUserEntityByEmail(@NonNull String email) {
        return userRepository.findByEmail(email);
    }

    @Autowired
    private ApplicationLoggerService log;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserPasswordHistoryService passwordHistoryService;

    @Autowired
    private UserActivityHistoryService activityHistoryService;

    @Autowired
    private RequestInfoUtil requestInfoUtil;

    @Autowired
    private com.event_management_system.repository.EventAttendeesRepository eventAttendeesRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RoleService roleService;

    @Autowired(required = false)
    private HttpServletRequest request;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        log.trace("[UserService] TRACE - createUser() called with email=" + userRequestDTO.getEmail());

        log.debug("[UserService] DEBUG - createUser() - Creating user entity from DTO");
        User user = userMapper.toEntity(userRequestDTO);
        user.recordCreation("system");

        if (user.getRole() != null && user.getRole().getId() != null) {
            Long roleId = user.getRole().getId();
            if (roleId != null) {
                boolean roleExists = roleRepository.existsById(roleId);
                if (!roleExists) {
                    log.warn("[UserService] WARN - Attempted to create user with non-existent roleId=" + roleId);
                    throw new RuntimeException("Role with ID " + roleId + " does not exist");
                }
                log.debug("[UserService] DEBUG - createUser() - Role validation passed for roleId=" + roleId);
            }
        }

        User savedUser = userRepository.save(user);
        User currentUser = getCurrentUser();

        log.info("[UserService] INFO - User created successfully: userId=" + savedUser.getId() + ", email="
                + savedUser.getEmail() + ", role="
                + (savedUser.getRole() != null ? savedUser.getRole().getName() : "None"));

        try {
            String password = savedUser.getPassword();
            if (password != null) {
                passwordHistoryService.recordPasswordChange(
                        savedUser, // User whose password was set
                        currentUser, // Created by (who created the user)
                        null, // No old password (new user)
                        password // New hashed password
                );
            }
        } catch (Exception e) {
            log.error("[UserService] ERROR - Failed to record password history: " + e.getMessage());
        }

        try {
            String ipAddress = getClientIp();
            String deviceId = getDeviceId();
            String sessionId = getSessionId();

            activityHistoryService.recordActivity(
                    currentUser,
                    UserActivityHistory.ActivityType.USER_CREATED,
                    "Created user: " + savedUser.getFullName() + " (" + savedUser.getEmail() + ") with role: " +
                            (savedUser.getRole() != null ? savedUser.getRole().getName() : "None"),
                    ipAddress,
                    deviceId,
                    sessionId);
        } catch (Exception e) {
            log.error("[UserService] ERROR - Failed to record user creation activity: " + e.getMessage());
        }

        return userMapper.toDto(savedUser);
    }

    @Transactional
    public UserResponseDTO selfRegisterAttendee(UserRequestDTO userRequestDTO) {
        log.trace("[UserService] TRACE - selfRegisterAttendee() called with email=" + userRequestDTO.getEmail());

        if (userRepository.findByEmail(userRequestDTO.getEmail()).isPresent()) {
            log.warn("[UserService] WARN - Self-registration failed: Email already exists - " + userRequestDTO.getEmail());
            throw new RuntimeException("Email already registered");
        }

        log.debug("[UserService] DEBUG - selfRegisterAttendee() - Fetching ATTENDEE role");
        Role attendeeRole = roleRepository.findByName("ATTENDEE")
                .orElseThrow(() -> {
                    log.error("[UserService] ERROR - ATTENDEE role not found in system");
                    return new RuntimeException("ATTENDEE role not found in system");
                });

        log.debug("[UserService] DEBUG - selfRegisterAttendee() - Creating user entity from DTO");
        User user = userMapper.toEntity(userRequestDTO);
        user.setRole(attendeeRole);
        user.recordCreation("self-registration");

        User savedUser = userRepository.save(user);

        log.info("[UserService] INFO - User self-registered successfully: userId=" + savedUser.getId() + ", email="
                + savedUser.getEmail() + ", role=ATTENDEE");

        log.debug("[UserService] DEBUG - selfRegisterAttendee() - Skipping password history (self-registration context)");

        try {
            String ipAddress = getClientIp();
            String deviceId = getDeviceId();
            String sessionId = getSessionId();

            activityHistoryService.recordActivity(
                    savedUser,
                    UserActivityHistory.ActivityType.USER_CREATED,
                    "User self-registered: " + savedUser.getFullName() + " (" + savedUser.getEmail() + ") with role: ATTENDEE",
                    ipAddress,
                    deviceId,
                    sessionId);
        } catch (Exception e) {
            log.error("[UserService] ERROR - Failed to record self-registration activity: " + e.getMessage());
        }

        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> getUserById(@NonNull Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<UserResponseDTO> getUserByEmail(@NonNull String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Long getUserIdByEmail(@NonNull String email) {
        return userRepository.findByEmail(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<UserResponseDTO> updateUser(@NonNull Long currentUserId, @NonNull Long targetUserId,
            UserUpdateRequestDTO userUpdateRequestDTO) {
        log.trace("[UserService] TRACE - updateUser() called with currentUserId=" + currentUserId + ", targetUserId="
                + targetUserId);

        log.debug("[UserService] DEBUG - updateUser() - Checking manage permission");
        if (!canManageUser(currentUserId, targetUserId)) {
            log.warn("[UserService] WARN - User " + currentUserId + " not authorized to update user " + targetUserId);
            throw new RuntimeException("You don't have permission to update this user");
        }

        return userRepository.findById(targetUserId).map(existingUser -> {
            String oldFullName = existingUser.getFullName();
            String oldEmail = existingUser.getEmail();

            log.debug("[UserService] DEBUG - updateUser() - Updating user entity");
            userMapper.updateEntity(userUpdateRequestDTO, existingUser);
            existingUser.recordUpdate("system");
            User updatedUser = userRepository.save(existingUser);

            log.info("[UserService] INFO - User updated successfully: userId=" + updatedUser.getId() + ", email="
                    + updatedUser.getEmail());

            try {
                User currentUser = getCurrentUser();
                String ipAddress = getClientIp();
                String deviceId = getDeviceId();
                String sessionId = getSessionId();

                String description = "Updated user: " + oldFullName;
                if (!oldEmail.equals(updatedUser.getEmail())) {
                    description += " | Email: " + oldEmail + " → " + updatedUser.getEmail();
                }
                if (!oldFullName.equals(updatedUser.getFullName())) {
                    description += " | Name: " + oldFullName + " → " + updatedUser.getFullName();
                }

                activityHistoryService.recordActivity(
                        currentUser,
                        UserActivityHistory.ActivityType.USER_UPDATED,
                        description,
                        ipAddress,
                        deviceId,
                        sessionId);
            } catch (Exception e) {
                log.error("[UserService] ERROR - Failed to record user update activity: " + e.getMessage());
            }

            return userMapper.toDto(updatedUser);
        });
    }

    @Transactional
    public boolean deleteUser(@NonNull Long currentUserId, @NonNull Long targetUserId) {
        log.trace("[UserService] TRACE - deleteUser() called with currentUserId=" + currentUserId + ", targetUserId=" + targetUserId);
        log.debug("[UserService] DEBUG - deleteUser() - Checking manage permission");
        if (!canManageUser(currentUserId, targetUserId)) {
            log.warn("[UserService] WARN - User " + currentUserId + " not authorized to delete user " + targetUserId);
            throw new RuntimeException("You don't have permission to delete this user");
        }

        return userRepository.findById(targetUserId).map(user -> {
            String deletedUserName = user.getFullName();
            String deletedUserEmail = user.getEmail();

            // Delete all related records first
            try {
                // Delete password history
                passwordHistoryService.deleteAllByUserId(targetUserId);
                // Delete login/logout history
                if (loginLogoutHistoryService != null) {
                    loginLogoutHistoryService.deleteAllByUserId(targetUserId);
                }
                // Delete activity history
                activityHistoryService.deleteAllByUserId(targetUserId);
                // Delete event attendees
                eventAttendeesRepository.deleteAll(eventAttendeesRepository.findByUser(user));
            } catch (Exception e) {
                log.error("[UserService] ERROR - Failed to delete related records for userId=" + targetUserId + ": " + e.getMessage());
                throw new RuntimeException("Failed to delete related records for userId=" + targetUserId, e);
            }

            log.debug("[UserService] DEBUG - deleteUser() - Permanently deleting user from database");
            userRepository.delete(user);

            log.info("[UserService] INFO - User deleted successfully: userId=" + targetUserId + ", email=" + deletedUserEmail);

            try {
                User currentUser = getCurrentUser();
                String ipAddress = getClientIp();
                String deviceId = getDeviceId();
                String sessionId = getSessionId();

                activityHistoryService.recordActivity(
                        currentUser,
                        UserActivityHistory.ActivityType.USER_DELETED,
                        "Deleted user: " + deletedUserName + " (" + deletedUserEmail + ") | ID: " + targetUserId,
                        ipAddress,
                        deviceId,
                        sessionId);
            } catch (Exception e) {
                log.error("[UserService] ERROR - Failed to record user deletion activity: " + e.getMessage());
            }

            return true;
        }).orElse(false);
    }

    @Transactional
    public void assignRoleToUser(@NonNull Long userId, @NonNull Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Role newRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));

        Role oldRole = user.getRole();
        Long oldRoleId = oldRole != null ? oldRole.getId() : null;

        user.setRole(newRole);
        user.recordUpdate("system");
        userRepository.save(user);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    recordRoleActivityAsync(userId, roleId, oldRoleId, UserActivityHistory.ActivityType.ROLE_ASSIGNED);
                } catch (Exception e) {
                    log.error("Failed to record role assignment activity: {}", e.getMessage());
                }
            }
        });
    }

    @Transactional
    public void removeRoleFromUser(@NonNull Long userId, @NonNull Long roleId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Role roleToRemove = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with ID: " + roleId));

        if (user.getRole() == null || !Objects.equals(user.getRole().getId(), roleId)) {
            throw new RuntimeException("User does not have role ID: " + roleId);
        }

        Long oldRoleId = roleToRemove.getId();

        user.setRole(null);
        user.recordUpdate("system");
        userRepository.save(user);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    recordRoleActivityAsync(userId, null, oldRoleId, UserActivityHistory.ActivityType.ROLE_REVOKED);
                } catch (Exception e) {
                    log.error("Failed to record role revocation activity: {}", e.getMessage());
                }
            }
        });
    }

    @Transactional
    public void addRoleToUser(@NonNull Long userId, @NonNull Long roleId) {
        assignRoleToUser(userId, roleId);
    }

    @Transactional(readOnly = true)
    public boolean hasRole(@NonNull Long userId, String roleName) {
        return userRepository.findById(userId)
                .map(user -> user.getRole() != null &&
                        Objects.equals(user.getRole().getName(), roleName))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(@NonNull Long userId, String permissionName) {
        return userRepository.findById(userId)
                .map(user -> {
                    log.info("Checking permission '{}' for user {}", permissionName, userId);
                    log.info("User role: {}", user.getRole() != null ? user.getRole().getName() : "null");

                    if (user.getRole() != null && user.getRole().getId() != null) {
                        Set<Permission> permissions = roleService.getPermissionsForRole(user.getRole().getId());
                        log.info("User has {} permissions: {}",
                                permissions.size(),
                                permissions.stream().map(Permission::getName)
                                        .collect(java.util.stream.Collectors.joining(", ")));

                        boolean hasIt = permissions.stream()
                                .anyMatch(permission -> Objects.equals(permission.getName(), permissionName));
                        log.info("Has permission '{}': {}", permissionName, hasIt);
                        return hasIt;
                    }

                    log.warn("User role or permissions is null - User ID: {}, Role: {}", userId, user.getRole() != null ? "null" : user.getRole().getName());
                    return false;
                })
                .orElse(false);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    private void recordRoleActivityAsync(Long targetUserId, Long newRoleId, Long oldRoleId,
            UserActivityHistory.ActivityType activityType) {
        try {
            User targetUser = userRepository.findById(targetUserId).orElse(null);
            if (targetUser == null) {
                log.warn("Target user {} not found for activity logging", targetUserId);
                return;
            }

            User currentUser = getCurrentUser();
            Role newRole = newRoleId != null ? roleRepository.findById(newRoleId).orElse(null) : null;
            Role oldRole = oldRoleId != null ? roleRepository.findById(oldRoleId).orElse(null) : null;

            String ipAddress = getClientIp();
            String deviceId = getDeviceId();
            String sessionId = getSessionId();

            String description;
            if (activityType == UserActivityHistory.ActivityType.ROLE_ASSIGNED) {
                description = "Assigned role '" + (newRole != null ? newRole.getName() : "Unknown") + "' to user: "
                        + targetUser.getFullName();
                if (oldRole != null) {
                    description += " | Previous role: " + oldRole.getName();
                }
            } else {
                description = "Revoked role '" + (oldRole != null ? oldRole.getName() : "Unknown") + "' from user: "
                        + targetUser.getFullName() + " (" + targetUser.getEmail() + ")";
            }

            activityHistoryService.recordActivity(
                    currentUser,
                    activityType,
                    description,
                    ipAddress,
                    deviceId,
                    sessionId);
        } catch (Exception e) {
            log.error("Failed to record role activity: {}", e.getMessage(), e);
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + email));
    }

    private String getClientIp() {
        if (request != null) {
            return requestInfoUtil.getClientIpAddress(request);
        }
        return "0.0.0.0";
    }

    private String getDeviceId() {
        if (request != null) {
            return requestInfoUtil.generateDeviceId(request);
        }
        return "unknown";
    }

    private String getSessionId() {
        try {
            if (request != null) {
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String jwt = authHeader.substring(7);
                    String tokenUuid = jwtService.getTokenUuidFromToken(jwt);
                    if (tokenUuid != null && !tokenUuid.isEmpty()) {
                        return tokenUuid;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract token UUID from JWT: {}", e.getMessage());
        }
        return "";
    }

    @Transactional(readOnly = true)
    public boolean canManageUser(@NonNull Long currentUserId, @NonNull Long targetUserId) {
        log.info("canManageUser called. currentUserId: {}, targetUserId: {}", currentUserId, targetUserId);
        
        if (hasPermission(currentUserId, "user.manage.all")) {
            log.info("User {} has user.manage.all permission - allowing management", currentUserId);
            return true;
        }

        if (hasPermission(currentUserId, "user.manage.own")) {
            log.info("User {} has user.manage.own permission - checking if target is Attendee", currentUserId);
            return userRepository.findById(targetUserId)
                    .map(targetUser -> targetUser.getRole() != null &&
                            Objects.equals(targetUser.getRole().getName(), "Attendee"))
                    .orElse(false);
        }

        boolean selfManagement = Objects.equals(currentUserId, targetUserId);
        log.info("User {} managing themselves: {}", currentUserId, selfManagement);
        return selfManagement;
    }

    @Transactional(readOnly = true)
    public boolean canManageEvent(@NonNull Long userId, @NonNull Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> {

                    if (hasPermission(userId, "event.manage.all")) {
                        return true;
                    }

                    if (hasPermission(userId, "event.manage.own")) {
                        return event.getOrganizer() != null && event.getOrganizer().getId().equals(userId);
                    }

                    return false;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canViewEvent(@NonNull Long userId, @NonNull Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    if (hasPermission(userId, "event.manage.all")) {
                        return true;
                    }

                    if (hasPermission(userId, "event.view.all")) {
                        return true;
                    }

                    if (event.getOrganizer() != null && event.getOrganizer().getId().equals(userId)) {
                        return true;
                    }

                    if (event.getVisibility() != null) {
                        return switch (event.getVisibility()) {
                            case PUBLIC -> hasPermission(userId, "event.view.public");
                            case PRIVATE -> {
                                var user = userRepository.findById(userId).orElse(null);
                                yield hasPermission(userId, "event.view.invited") &&
                                        (user != null && isUserInvitedToEvent(event, userId));
                            }
                            default -> false;
                        };
                    }

                    return false;
                })
                .orElse(false);
    }

    private boolean isUserInvitedToEvent(com.event_management_system.entity.Event event, @NonNull Long userId) {
        return userRepository.findById(userId)
                .map(user -> eventAttendeesRepository.existsByEventAndEmail(event, user.getEmail()))
                .orElse(false);
    }

    @PostConstruct
    @Transactional
    public void initializeDefaultUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        if (!userRepository.findByEmail("superadmin@ems.com").isPresent()) {
            User superAdmin = new User();
            superAdmin.setFullName("Super Admin");
            superAdmin.setEmail("superadmin@ems.com");
            superAdmin.setPassword(passwordEncoder.encode("SuperAdmin@123")); // BCrypt encoded
            superAdmin.recordCreation("system");

            Optional<Role> superAdminRole = roleRepository.findByName("SuperAdmin");
            if (superAdminRole.isPresent()) {
                superAdmin.setRole(superAdminRole.get());
            } else {
                throw new RuntimeException(
                        "SuperAdmin role not found in database. Please ensure RoleService has initialized.");
            }

            userRepository.save(superAdmin);
        }
    }

   
    public java.util.Map<String, Object> createAutoAccountForInvitee(String email, String fullName) {
        log.trace("[UserService] TRACE - createAutoAccountForInvitee() called with email={}", email);

        if (userRepository.findByEmail(email).isPresent()) {
            log.warn("[UserService] WARN - User already exists with email={}", email);
            throw new RuntimeException("User already exists with this email");
        }

        log.debug("[UserService] DEBUG - Generating temporary password for email={}", email);
        
        String tempPassword = generateTemporaryPassword();
        log.debug("[UserService] DEBUG - Generated password length: {}", tempPassword.length());

        Role userRole = roleRepository.findByName("Attendee")
                .orElseThrow(() -> {
                    log.warn("[UserService] WARN - Default 'Attendee' role not found");
                    return new RuntimeException("Default 'Attendee' role not found in system");
                });

        log.debug("[UserService] DEBUG - Found User role: {}", userRole.getId());

        User newUser = new User();
        newUser.setEmail(email.toLowerCase().trim());
        newUser.setFullName(fullName.trim());
        newUser.setPassword(passwordEncoder.encode(tempPassword)); // Hash the temporary password
        newUser.setRole(userRole);
        newUser.recordCreation("system");

        log.debug("[UserService] DEBUG - Saving user with email={}", newUser.getEmail());
        
        User savedUser = userRepository.save(newUser);

        log.info("[UserService] INFO - Auto account created: userId={}, email={}, role=Attendee, tempPassword=***",
                savedUser.getId(), savedUser.getEmail());

        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("user", savedUser);
        result.put("password", tempPassword);
        
        log.debug("[UserService] DEBUG - Returning account info map with user={} and password", savedUser.getId());
        
        return result;
    }

    
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder password = new StringBuilder();
        java.util.Random random = new java.util.Random();
        
        for (int i = 0; i < 10; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }
}
