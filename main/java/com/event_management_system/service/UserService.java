package com.event_management_system.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.dto.UserRequestDTO;
import com.event_management_system.dto.UserResponseDTO;
import com.event_management_system.entity.Role;
import com.event_management_system.entity.User;
import com.event_management_system.mapper.UserMapper;
import com.event_management_system.repository.EventRepository;
import com.event_management_system.repository.RoleRepository;
import com.event_management_system.repository.UserRepository;

import jakarta.annotation.PostConstruct;

@Service
public class UserService {

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

    @Transactional
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        User user = userMapper.toEntity(userRequestDTO);
        user.recordCreation("system");
        
        // Validate that the role exists before assigning
        if (user.getRole() != null && user.getRole().getId() != null) {
            Long roleId = user.getRole().getId();
            if (roleId != null) {
                boolean roleExists = roleRepository.existsById(roleId);
                if (!roleExists) {
                    throw new RuntimeException("Role with ID " + roleId + " does not exist");
                }
            }
        }
        
        User savedUser = userRepository.save(user);
        
        // Record password creation in history (for new user, no old password)
        try {
            String password = savedUser.getPassword();
            if (password != null) {
                passwordHistoryService.recordPasswordChange(
                    savedUser,          // User whose password was set
                    savedUser,          // Created by (SuperAdmin who created the user)
                    null,               // No old password (new user)
                    password            // New hashed password
                );
            }
        } catch (Exception e) {
            // Don't fail user creation if history recording fails
            // Just log the error
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
    public List<UserResponseDTO> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<UserResponseDTO> updateUser(@NonNull Long currentUserId, @NonNull Long targetUserId, UserRequestDTO userRequestDTO) {
        // Check if current user can manage the target user
        if (!canManageUser(currentUserId, targetUserId)) {
            throw new RuntimeException("You don't have permission to update this user");
        }
        
        return userRepository.findById(targetUserId).map(existingUser -> {
            userMapper.updateEntity(userRequestDTO, existingUser);
            existingUser.recordUpdate("system");
            User updatedUser = userRepository.save(existingUser);
            return userMapper.toDto(updatedUser);
        });
    }

    @Transactional
    public boolean deleteUser(@NonNull Long currentUserId, @NonNull Long targetUserId) {
        // Check if current user can manage the target user
        if (!canManageUser(currentUserId, targetUserId)) {
            throw new RuntimeException("You don't have permission to delete this user");
        }
        
        return userRepository.findById(targetUserId).map(user -> {
            user.markDeleted();
            userRepository.save(user);
            return true;
        }).orElse(false);
    }
    
    @Transactional
    public boolean assignRoleToUser(@NonNull Long userId, @NonNull Long roleId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        
        if (userOpt.isPresent() && roleOpt.isPresent()) {
            User user = userOpt.get();
            
            // User can only have one role, so replace existing role
            user.setRole(roleOpt.get());
            user.recordUpdate("system");
            userRepository.save(user);
            return true;
        }
        return false;
    }
    
    @Transactional
    public boolean removeRoleFromUser(@NonNull Long userId, @NonNull Long roleId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        
        if (userOpt.isPresent() && roleOpt.isPresent()) {
            User user = userOpt.get();
            
            // User can only have one role, so remove the role if it matches
            if (user.getRole() != null && Objects.equals(user.getRole().getId(), roleId)) {
                user.setRole(null);
                user.recordUpdate("system");
                userRepository.save(user);
                return true;
            }
        }
        return false;
    }
    
    // Alias methods to match UserController expectations
    @Transactional
    public boolean addRoleToUser(@NonNull Long userId, @NonNull Long roleId) {
        return assignRoleToUser(userId, roleId);
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
                .map(user -> user.getRole() != null &&
                       user.getRole().getPermissions() != null &&
                       user.getRole().getPermissions().stream()
                               .anyMatch(permission -> Objects.equals(permission.getName(), permissionName)))
                .orElse(false);
    }
    
    @Transactional(readOnly = true)
    public boolean canManageUser(@NonNull Long currentUserId, @NonNull Long targetUserId) {
        // SuperAdmin can manage all users
        if (hasPermission(currentUserId, "user.manage.all")) {
            return true;
        }
        
        // Admin can only manage attendees (not other admins or super admins)
        if (hasPermission(currentUserId, "user.manage.own")) {
            return userRepository.findById(targetUserId)
                    .map(targetUser -> targetUser.getRole() != null &&
                           Objects.equals(targetUser.getRole().getName(), "Attendee"))
                    .orElse(false);
        }
        
        // Users can manage themselves
        return Objects.equals(currentUserId, targetUserId);
    }
    
    
    @Transactional(readOnly = true)
    public boolean canManageEvent(@NonNull Long userId, @NonNull Long eventId) {
        // Delegate to EventService for consistent logic
        return eventRepository.findById(eventId)
                .map(event -> {
                    // Use EventService's internal method by creating a wrapper
                    // SuperAdmin can manage all events
                    if (hasPermission(userId, "event.manage.all")) {
                        return true;
                    }
                    
                    // Admin can only manage their own events
                    if (hasPermission(userId, "event.manage.own")) {
                        return event.getOrganizer() != null && event.getOrganizer().getId().equals(userId);
                    }
                    
                    return false;
                })
                .orElse(false);
    }
    
    @Transactional(readOnly = true)
    public boolean canViewEvent(@NonNull Long userId, @NonNull Long eventId) {
        // Delegate to EventService for consistent logic
        return eventRepository.findById(eventId)
                .map(event -> {
                    // SuperAdmin can view all events
                    if (hasPermission(userId, "event.manage.all")) {
                        return true;
                    }
                    
                    // Admin can view all events
                    if (hasPermission(userId, "event.view.all")) {
                        return true;
                    }
                    
                    // Event organizer can view their own events
                    if (event.getOrganizer() != null && event.getOrganizer().getId().equals(userId)) {
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
                })
                .orElse(false);
    }
    
    private boolean isUserInvitedToEvent(com.event_management_system.entity.Event event, @NonNull Long userId) {
        if (event.getAttendees() == null) {
            return false;
        }
        
        return event.getAttendees().stream()
                .anyMatch(attendee -> attendee.getId().equals(userId));
    }

    // Initialize default SuperAdmin user only (system bootstrapping)
    @PostConstruct
    @Transactional
    public void initializeDefaultUsers() {
        // Check if users already exist
        if (userRepository.count() > 0) {
            return;
        }

        // Create only SuperAdmin user - other users must be created by SuperAdmin
        if (!userRepository.findByEmail("superadmin@ems.com").isPresent()) {
            User superAdmin = new User();
            superAdmin.setFullName("Super Admin");
            superAdmin.setEmail("superadmin@ems.com");
            superAdmin.setPassword("password"); // In production, this should be encrypted
            superAdmin.recordCreation("system");
            
            // Assign ADMIN role (using the role created in data.sql)
            Optional<Role> adminRole = roleRepository.findByName("ADMIN");
            if (adminRole.isPresent()) {
                superAdmin.setRole(adminRole.get());
            } else {
                throw new RuntimeException("ADMIN role not found in database. Please ensure data.sql has been executed.");
            }
            
            userRepository.save(superAdmin);
        }
        // Admin and Attendee users will be created by SuperAdmin through API
    }
}