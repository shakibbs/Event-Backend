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

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user with provided details")
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody @NonNull UserRequestDTO userRequestDTO) {
        
        UserResponseDTO createdUser = userService.createUser(userRequestDTO);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
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
            // Get current user ID from authentication
            User currentUser = (User) authentication.getPrincipal();
            return userService.updateUser(currentUser.getId(), userId, userRequestDTO)
                    .map(user -> new ResponseEntity<>(user, HttpStatus.OK))
                    .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete a user", description = "Permanently deletes a user by their ID")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of user to delete") @PathVariable @NonNull Long userId,
            Authentication authentication) {
        
        try {
            // Get current user ID from authentication
            User currentUser = (User) authentication.getPrincipal();
            boolean deleted = userService.deleteUser(currentUser.getId(), userId);
            return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                          : new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }
    
    @PostMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Add role to user", description = "Assigns a role to a user")
    public ResponseEntity<Void> addRoleToUser(
            @Parameter(description = "ID of user") @PathVariable @NonNull Long userId,
            @Parameter(description = "ID of role to assign") @PathVariable @NonNull Long roleId) {
        
        userService.assignRoleToUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{userId}/roles/{roleId}")
    @Operation(summary = "Remove role from user", description = "Removes a role from a user")
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "ID of user") @PathVariable @NonNull Long userId,
            @Parameter(description = "ID of role to remove") @PathVariable @NonNull Long roleId) {
        
        userService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }
}