package com.event_management_system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.web.bind.annotation.RestController;

import com.event_management_system.dto.UserRequestDTO;
import com.event_management_system.dto.UserResponseDTO;
import com.event_management_system.dto.UserUpdateRequestDTO;
import com.event_management_system.entity.User;
import com.event_management_system.service.ApplicationLoggerService;
import com.event_management_system.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationLoggerService log;

    @Autowired
    private com.event_management_system.service.ReportService reportService;

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user with provided details")
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody @NonNull UserRequestDTO userRequestDTO) {
        
        try {
            log.trace("[UserController] TRACE - createUser() called with email=" + userRequestDTO.getEmail() + ", fullName=" + userRequestDTO.getFullName());
            log.debug("[UserController] DEBUG - createUser() - POST /api/users - Creating user: email=" + userRequestDTO.getEmail() + ", fullName=" + userRequestDTO.getFullName());
            UserResponseDTO createdUser = userService.createUser(userRequestDTO);
            log.info("[UserController] INFO - createUser() - User created successfully: userId=" + createdUser.getId() + ", email=" + createdUser.getEmail() + ", fullName=" + createdUser.getFullName());
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("[UserController] ERROR - createUser() - Failed to create user: email=" + userRequestDTO.getEmail(), e);
            throw e;
        }
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by their ID")
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "ID of user to retrieve") @PathVariable @NonNull Long userId) {
        
        return userService.getUserById(userId)
                .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves a list of all users")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        
        List<UserResponseDTO> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/email/{email}")
    @Operation(summary = "Get user by email", description = "Retrieves a user by their email address")
    public ResponseEntity<UserResponseDTO> getUserByEmail(
            @Parameter(description = "Email address of user to retrieve") @PathVariable @NonNull String email) {
        
        return userService.getUserByEmail(email)
                .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update a user", description = "Updates an existing user with provided details")
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "ID of user to update") @PathVariable @NonNull Long userId,
            @Valid @RequestBody @NonNull UserUpdateRequestDTO userUpdateRequestDTO,
            Authentication authentication) {
        
        try {
            log.trace("[UserController] TRACE - updateUser() called with userId=" + userId + ", email=" + userUpdateRequestDTO.getEmail());
            log.debug("[UserController] DEBUG - updateUser() - PUT /api/users/" + userId + " - Updating user: email=" + userUpdateRequestDTO.getEmail());
            String email = authentication.getName();
                User currentUser = userService.getUserEntityByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Current user not found: " + email));
            log.debug("[UserController] DEBUG - updateUser() - User authenticated: userId=" + currentUser.getId());
            var result = userService.updateUser(currentUser.getId(), userId, userUpdateRequestDTO);
           
            if (result.isPresent()) {
                log.info("[UserController] INFO - updateUser() - User updated successfully: userId=" + userId + ", email=" + result.get().getEmail());
                return new ResponseEntity<>(result.get(), HttpStatus.OK);
            } else {
                log.warn("[UserController] WARN - updateUser() - User not found for update: userId=" + userId + ", requestedBy=" + currentUser.getId());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (RuntimeException e) {
            log.warn("[UserController] WARN - updateUser() - Access denied for user update: userId=" + userId + ", error=" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("[UserController] ERROR - updateUser() - Failed to update user: userId=" + userId, e);
            throw e;
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete a user", description = "Permanently deletes a user by their ID")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of user to delete") @PathVariable @NonNull Long userId,
            Authentication authentication) {
        
        try {
            log.trace("[UserController] TRACE - deleteUser() called with userId=" + userId + ", timestamp=" + System.currentTimeMillis());
            log.debug("[UserController] DEBUG - deleteUser() - DELETE /api/users/" + userId + " - Deleting user");
            String email = authentication.getName();
            log.debug("[UserController] DEBUG - deleteUser() - Authenticated email: " + email);
            User currentUser = userService.getUserEntityByEmail(email)
                .orElseThrow(() -> new RuntimeException("Current user not found: " + email));
            log.debug("[UserController] DEBUG - deleteUser() - User authenticated: userId=" + currentUser.getId() + ", role=" + (currentUser.getRole() != null ? currentUser.getRole().getName() : "null"));
            boolean deleted = userService.deleteUser(currentUser.getId(), userId);
           
            if (deleted) {
                log.info("[UserController] INFO - deleteUser() - User deleted successfully: userId=" + userId + ", deletedBy=" + currentUser.getId());
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                log.warn("[UserController] WARN - deleteUser() - User not found for deletion: userId=" + userId + ", deletedBy=" + currentUser.getId());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (RuntimeException e) {
            log.warn("[UserController] WARN - deleteUser() - Access denied for user deletion: userId=" + userId + ", error=" + e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            log.error("[UserController] ERROR - deleteUser() - Failed to delete user: userId=" + userId, e);
            throw e;
        }
    }
    
    @PostMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Add role to user", description = "Assigns a role to a user")
    public ResponseEntity<Void> addRoleToUser(
            @Parameter(description = "ID of user") @PathVariable @NonNull Long userId,
            @Parameter(description = "ID of role to assign") @PathVariable @NonNull Long roleId) {
        
        try {
            log.trace("[UserController] TRACE - addRoleToUser() called with userId=" + userId + ", roleId=" + roleId + ", timestamp=" + System.currentTimeMillis());
            log.debug("[UserController] DEBUG - addRoleToUser() - POST /api/users/" + userId + "/roles/" + roleId + " - Adding role to user");
            userService.assignRoleToUser(userId, roleId);
            log.info("[UserController] INFO - addRoleToUser() - Role added to user successfully: userId=" + userId + ", roleId=" + roleId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("[UserController] ERROR - addRoleToUser() - Failed to add role to user: userId=" + userId + ", roleId=" + roleId, e);
            throw e;
        }
    }
    
    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Remove role from user", description = "Removes a role from a user")
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "ID of user") @PathVariable @NonNull Long userId,
            @Parameter(description = "ID of role to remove") @PathVariable @NonNull Long roleId) {
        
        try {
            log.trace("[UserController] TRACE - removeRoleFromUser() called with userId=" + userId + ", roleId=" + roleId + ", timestamp=" + System.currentTimeMillis());
            log.debug("[UserController] DEBUG - removeRoleFromUser() - DELETE /api/users/" + userId + "/roles/" + roleId + " - Removing role from user");
            userService.removeRoleFromUser(userId, roleId);
            log.info("[UserController] INFO - removeRoleFromUser() - Role removed from user successfully: userId=" + userId + ", roleId=" + roleId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("[UserController] ERROR - removeRoleFromUser() - Failed to remove role from user: userId=" + userId + ", roleId=" + roleId, e);
            throw e;
        }
    }
    
    @GetMapping(value = "/download/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Download users as PDF", description = "Downloads a PDF of all users visible to the current user, filtered by role/permission.")
    public ResponseEntity<byte[]> downloadUsersPdf(Authentication authentication) {
    log.info("PDF user export endpoint called. Authentication: {}", authentication != null ? authentication.toString() : "null");
        if (authentication == null) {
            log.warn("No authentication provided to /api/users/download/pdf");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } else {
            log.info("Authenticated principal: {} (authorities: {})", authentication.getName(), authentication.getAuthorities().toString());
        }

        String email = authentication.getName();
        User currentUser = userService.getUserEntityByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        boolean canExportAll = userService.hasPermission(currentUser.getId(), "user.manage.all");
        boolean canExportOwn = userService.hasPermission(currentUser.getId(), "user.manage.own");
        boolean canExport = userService.hasPermission(currentUser.getId(), "user.export") || canExportAll || canExportOwn;
        if (!canExport) {
            log.warn("User {} does not have permission to export user list", currentUser.getId());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        java.util.List<UserResponseDTO> allUsers = userService.getAllUsers();
        java.util.List<UserResponseDTO> filteredUsers;
        if (canExportAll) {
            filteredUsers = allUsers;
        } else if (canExportOwn) {
            filteredUsers = allUsers.stream()
                .filter(u -> u.getRole() != null && u.getRole().getName() != null && u.getRole().getName().equalsIgnoreCase("Attendee"))
                .collect(java.util.stream.Collectors.toList());
        } else {
            filteredUsers = java.util.Collections.emptyList();
        }

        java.util.Map<String, Object> parameters = new java.util.HashMap<>();
        parameters.put("generatedBy", currentUser.getFullName());
        parameters.put("generatedAt", java.time.LocalDateTime.now().toString());

        try {
            // Use the new generateUsersPdf for users
            byte[] pdfBytes = reportService.generateUsersPdf(filteredUsers, parameters);
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=users_list.pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdfBytes);
        } catch (net.sf.jasperreports.engine.JRException | IllegalArgumentException e) { // multicatch is correct for these exceptions
            log.error("Failed to generate PDF report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
