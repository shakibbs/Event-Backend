package com.event_management_system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.event_management_system.dto.RoleRequestDTO;
import com.event_management_system.dto.RoleResponseDTO;
import com.event_management_system.service.RoleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/roles")
@Tag(name = "Role Management", description = "APIs for managing roles")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @PostMapping
    @Operation(summary = "Create a new role", description = "Creates a new role with provided details")
    public ResponseEntity<RoleResponseDTO> createRole(
            @Valid @RequestBody @NonNull RoleRequestDTO roleRequestDTO) {
        
        RoleResponseDTO createdRole = roleService.createRole(roleRequestDTO);
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Retrieves a role by its ID")
    public ResponseEntity<RoleResponseDTO> getRoleById(
            @Parameter(description = "ID of role to retrieve") @PathVariable @NonNull Long id) {
        return roleService.getRoleById(id)
                .map(role -> new ResponseEntity<>(role, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping
    @Operation(summary = "Get all roles", description = "Retrieves a list of all active roles")
    public ResponseEntity<List<RoleResponseDTO>> getAllRoles() {
        
        List<RoleResponseDTO> roles = roleService.getAllRoles();
        return new ResponseEntity<>(roles, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a role", description = "Updates an existing role with provided details")
    public ResponseEntity<RoleResponseDTO> updateRole(
            @Parameter(description = "ID of role to update") @PathVariable @NonNull Long id,
            @Valid @RequestBody @NonNull RoleRequestDTO roleRequestDTO) {
        
        return roleService.updateRole(id, roleRequestDTO)
                .map(role -> new ResponseEntity<>(role, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a role", description = "Soft deletes a role by its ID")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "ID of role to delete") @PathVariable @NonNull Long id) {
        
        boolean deleted = roleService.deleteRole(id);
        return deleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
                      : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    
    @PostMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Add permission to role", description = "Assigns a permission to a role")
    public ResponseEntity<Void> addPermissionToRole(
            @Parameter(description = "ID of role") @PathVariable @NonNull Long roleId,
            @Parameter(description = "ID of permission to assign") @PathVariable @NonNull Long permissionId) {
        
        boolean added = roleService.addPermissionToRole(roleId, permissionId);
        return added ? new ResponseEntity<>(HttpStatus.OK)
                     : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
    
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Remove permission from role", description = "Removes a permission from a role")
    public ResponseEntity<Void> removePermissionFromRole(
            @Parameter(description = "ID of role") @PathVariable @NonNull Long roleId,
            @Parameter(description = "ID of permission to remove") @PathVariable @NonNull Long permissionId) {
        
        boolean removed = roleService.removePermissionFromRole(roleId, permissionId);
        return removed ? new ResponseEntity<>(HttpStatus.OK)
                       : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}