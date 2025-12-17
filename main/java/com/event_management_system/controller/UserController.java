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
    private ApplicationLoggerService logger;

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user with provided details")
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody @NonNull UserRequestDTO userRequestDTO) {
        
        try {
            logger.traceWithContext("UserController", "createUser() called with email={}, fullName={}", userRequestDTO.getEmail(), userRequestDTO.getFullName());
            logger.debugWithContext("UserController", "POST /api/users - Creating user: email={}, fullName={}", userRequestDTO.getEmail(), userRequestDTO.getFullName());
            UserResponseDTO createdUser = userService.createUser(userRequestDTO);
            logger.infoWithContext("UserController", "User created successfully: userId={}, email={}, fullName={}", createdUser.getId(), createdUser.getEmail(), createdUser.getFullName());
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.errorWithContext("UserController", "Failed to create user: email={}", e);
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
            @Valid @RequestBody @NonNull UserRequestDTO userRequestDTO,
            Authentication authentication) {
        
        try {
            logger.traceWithContext("UserController", "updateUser() called with userId={}, email={}", userId, userRequestDTO.getEmail());
            logger.debugWithContext("UserController", "PUT /api/users/{} - Updating user: email={}", userId, userRequestDTO.getEmail());
            User currentUser = (User) authentication.getPrincipal();
            logger.debugWithContext("UserController", "User authenticated: userId={}", currentUser.getId());
            var result = userService.updateUser(currentUser.getId(), userId, userRequestDTO);
           
            if (result.isPresent()) {
                logger.infoWithContext("UserController", "User updated successfully: userId={}, email={}", userId, result.get().getEmail());
                return new ResponseEntity<>(result.get(), HttpStatus.OK);
            } else {
                logger.warnWithContext("UserController", "User not found for update: userId={}, requestedBy={}", userId, currentUser.getId());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (RuntimeException e) {
            logger.warnWithContext("UserController", "Access denied for user update: userId={}, error={}", userId, e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            logger.errorWithContext("UserController", "Failed to update user: userId={}", e);
            throw e;
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete a user", description = "Permanently deletes a user by their ID")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of user to delete") @PathVariable @NonNull Long userId,
            Authentication authentication) {
        
        try {
            logger.traceWithContext("UserController", "deleteUser() called with userId={}, timestamp={}", userId, System.currentTimeMillis());
            logger.debugWithContext("UserController", "DELETE /api/users/{} - Deleting user", userId);
            User currentUser = (User) authentication.getPrincipal();
            logger.debugWithContext("UserController", "User authenticated: userId={}", currentUser.getId());
            boolean deleted = userService.deleteUser(currentUser.getId(), userId);
           
            if (deleted) {
                logger.infoWithContext("UserController", "User deleted successfully: userId={}, deletedBy={}", userId, currentUser.getId());
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                logger.warnWithContext("UserController", "User not found for deletion: userId={}, deletedBy={}", userId, currentUser.getId());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (RuntimeException e) {
            logger.warnWithContext("UserController", "Access denied for user deletion: userId={}, error={}", userId, e.getMessage());
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            logger.errorWithContext("UserController", "Failed to delete user: userId={}", e);
            throw e;
        }
    }
    
    @PostMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Add role to user", description = "Assigns a role to a user")
    public ResponseEntity<Void> addRoleToUser(
            @Parameter(description = "ID of user") @PathVariable @NonNull Long userId,
            @Parameter(description = "ID of role to assign") @PathVariable @NonNull Long roleId) {
        
        try {
            logger.traceWithContext("UserController", "addRoleToUser() called with userId={}, roleId={}, timestamp={}", userId, roleId, System.currentTimeMillis());
            logger.debugWithContext("UserController", "POST /api/users/{}/roles/{} - Adding role to user", userId, roleId);
            userService.assignRoleToUser(userId, roleId);
            logger.infoWithContext("UserController", "Role added to user successfully: userId={}, roleId={}", userId, roleId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.errorWithContext("UserController", "Failed to add role to user: userId={}, roleId={}", e);
            throw e;
        }
    }
    
    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Remove role from user", description = "Removes a role from a user")
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "ID of user") @PathVariable @NonNull Long userId,
            @Parameter(description = "ID of role to remove") @PathVariable @NonNull Long roleId) {
        
        try {
            logger.traceWithContext("UserController", "removeRoleFromUser() called with userId={}, roleId={}, timestamp={}", userId, roleId, System.currentTimeMillis());
            logger.debugWithContext("UserController", "DELETE /api/users/{}/roles/{} - Removing role from user", userId, roleId);
            userService.removeRoleFromUser(userId, roleId);
            logger.infoWithContext("UserController", "Role removed from user successfully: userId={}, roleId={}", userId, roleId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            logger.errorWithContext("UserController", "Failed to remove role from user: userId={}, roleId={}", e);
            throw e;
        }
    }
}