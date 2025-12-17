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
import com.event_management_system.service.ApplicationLoggerService;
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
    
    @Autowired
    private ApplicationLoggerService logger;

    @PostMapping
    @Operation(summary = "Create a new role", description = "Creates a new role with provided details")
    public ResponseEntity<RoleResponseDTO> createRole(
            @Valid @RequestBody @NonNull RoleRequestDTO roleRequestDTO) {
        
        try {
            logger.traceWithContext("RoleController", "createRole() called with name={}, timestamp={}", roleRequestDTO.getName(), System.currentTimeMillis());
            logger.debugWithContext("RoleController", "POST /api/roles - Creating role: name={}", roleRequestDTO.getName());
            RoleResponseDTO createdRole = roleService.createRole(roleRequestDTO);
            logger.infoWithContext("RoleController", "Role created successfully: roleId={}, name={}", createdRole.getId(), createdRole.getName());
            return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.errorWithContext("RoleController", "Failed to create role: name={}", e);
            throw e;
        }
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
        
        try {
            logger.traceWithContext("RoleController", "updateRole() called with roleId={}, name={}", id, roleRequestDTO.getName());
            logger.debugWithContext("RoleController", "PUT /api/roles/{} - Updating role: name={}", id, roleRequestDTO.getName());
            var result = roleService.updateRole(id, roleRequestDTO);
            
            if (result.isPresent()) {
                logger.infoWithContext("RoleController", "Role updated successfully: roleId={}, name={}", id, result.get().getName());
                return new ResponseEntity<>(result.get(), HttpStatus.OK);
            } else {
                logger.warnWithContext("RoleController", "Role not found for update: roleId={}", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.errorWithContext("RoleController", "Failed to update role: roleId={}", e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a role", description = "Soft deletes a role by its ID")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "ID of role to delete") @PathVariable @NonNull Long id) {
        
        try {
            logger.traceWithContext("RoleController", "deleteRole() called with roleId={}, timestamp={}", id, System.currentTimeMillis());
            logger.debugWithContext("RoleController", "DELETE /api/roles/{} - Deleting role", id);
            boolean deleted = roleService.deleteRole(id);
            
            if (deleted) {
                logger.infoWithContext("RoleController", "Role deleted successfully: roleId={}", id);
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                logger.warnWithContext("RoleController", "Role not found for deletion: roleId={}", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.errorWithContext("RoleController", "Failed to delete role: roleId={}", e);
            throw e;
        }
    }
    
    @PostMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Add permission to role", description = "Assigns a permission to a role")
    public ResponseEntity<Void> addPermissionToRole(
            @Parameter(description = "ID of role") @PathVariable @NonNull Long roleId,
            @Parameter(description = "ID of permission to assign") @PathVariable @NonNull Long permissionId) {
        
        try {
            logger.traceWithContext("RoleController", "addPermissionToRole() called with roleId={}, permissionId={}, timestamp={}", roleId, permissionId, System.currentTimeMillis());
            logger.debugWithContext("RoleController", "POST /api/roles/{}/permissions/{} - Adding permission to role", roleId, permissionId);
            boolean added = roleService.addPermissionToRole(roleId, permissionId);
            
            if (added) {
                logger.infoWithContext("RoleController", "Permission added to role successfully: roleId={}, permissionId={}", roleId, permissionId);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                logger.warnWithContext("RoleController", "Role or permission not found: roleId={}, permissionId={}", roleId, permissionId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.errorWithContext("RoleController", "Failed to add permission to role: roleId={}, permissionId={}", e);
            throw e;
        }
    }
    
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Remove permission from role", description = "Removes a permission from a role")
    public ResponseEntity<Void> removePermissionFromRole(
            @Parameter(description = "ID of role") @PathVariable @NonNull Long roleId,
            @Parameter(description = "ID of permission to remove") @PathVariable @NonNull Long permissionId) {
        
        try {
            logger.traceWithContext("RoleController", "removePermissionFromRole() called with roleId={}, permissionId={}, timestamp={}", roleId, permissionId, System.currentTimeMillis());
            logger.debugWithContext("RoleController", "DELETE /api/roles/{}/permissions/{} - Removing permission from role", roleId, permissionId);
            boolean removed = roleService.removePermissionFromRole(roleId, permissionId);
            
            if (removed) {
                logger.infoWithContext("RoleController", "Permission removed from role successfully: roleId={}, permissionId={}", roleId, permissionId);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                logger.warnWithContext("RoleController", "Role or permission not found for removal: roleId={}, permissionId={}", roleId, permissionId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.errorWithContext("RoleController", "Failed to remove permission from role: roleId={}, permissionId={}", e);
            throw e;
        }
    }
}