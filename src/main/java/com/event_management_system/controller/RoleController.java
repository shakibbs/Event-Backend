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
    private ApplicationLoggerService log;

    @PostMapping
    @Operation(summary = "Create a new role", description = "Creates a new role with provided details")
    public ResponseEntity<RoleResponseDTO> createRole(
            @Valid @RequestBody @NonNull RoleRequestDTO roleRequestDTO) {
        
        try {
            log.trace("[RoleController] TRACE - createRole() called with name=" + roleRequestDTO.getName() + ", timestamp=" + System.currentTimeMillis());
            log.debug("[RoleController] DEBUG - createRole() - POST /api/roles - Creating role: name=" + roleRequestDTO.getName());
            RoleResponseDTO createdRole = roleService.createRole(roleRequestDTO);
            log.info("[RoleController] INFO - createRole() - Role created successfully: roleId=" + createdRole.getId() + ", name=" + createdRole.getName());
            return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("[RoleController] ERROR - createRole() - Failed to create role: name=" + roleRequestDTO.getName(), e);
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
            log.trace("[RoleController] TRACE - updateRole() called with roleId=" + id + ", name=" + roleRequestDTO.getName());
            log.debug("[RoleController] DEBUG - updateRole() - PUT /api/roles/" + id + " - Updating role: name=" + roleRequestDTO.getName());
            var result = roleService.updateRole(id, roleRequestDTO);
            
            if (result.isPresent()) {
                log.info("[RoleController] INFO - updateRole() - Role updated successfully: roleId=" + id + ", name=" + result.get().getName());
                return new ResponseEntity<>(result.get(), HttpStatus.OK);
            } else {
                log.warn("[RoleController] WARN - updateRole() - Role not found for update: roleId=" + id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("[RoleController] ERROR - updateRole() - Failed to update role: roleId=" + id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a role", description = "Soft deletes a role by its ID")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "ID of role to delete") @PathVariable @NonNull Long id) {
        
        try {
            log.trace("[RoleController] TRACE - deleteRole() called with roleId=" + id + ", timestamp=" + System.currentTimeMillis());
            log.debug("[RoleController] DEBUG - deleteRole() - DELETE /api/roles/" + id + " - Deleting role");
            boolean deleted = roleService.deleteRole(id);
            
            if (deleted) {
                log.info("[RoleController] INFO - deleteRole() - Role deleted successfully: roleId=" + id);
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                log.warn("[RoleController] WARN - deleteRole() - Role not found for deletion: roleId=" + id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("[RoleController] ERROR - deleteRole() - Failed to delete role: roleId=" + id, e);
            throw e;
        }
    }
    
    @PostMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Add permission to role", description = "Assigns a permission to a role")
    public ResponseEntity<Void> addPermissionToRole(
            @Parameter(description = "ID of role") @PathVariable @NonNull Long roleId,
            @Parameter(description = "ID of permission to assign") @PathVariable @NonNull Long permissionId) {
        
        try {
            log.trace("[RoleController] TRACE - addPermissionToRole() called with roleId=" + roleId + ", permissionId=" + permissionId + ", timestamp=" + System.currentTimeMillis());
            log.debug("[RoleController] DEBUG - addPermissionToRole() - POST /api/roles/" + roleId + "/permissions/" + permissionId + " - Adding permission to role");
            boolean added = roleService.addPermissionToRole(roleId, permissionId);
            
            if (added) {
                log.info("[RoleController] INFO - addPermissionToRole() - Permission added to role successfully: roleId=" + roleId + ", permissionId=" + permissionId);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                log.warn("[RoleController] WARN - addPermissionToRole() - Role or permission not found: roleId=" + roleId + ", permissionId=" + permissionId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("[RoleController] ERROR - addPermissionToRole() - Failed to add permission to role: roleId=" + roleId + ", permissionId=" + permissionId, e);
            throw e;
        }
    }
    
    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    @Operation(summary = "Remove permission from role", description = "Removes a permission from a role")
    public ResponseEntity<Void> removePermissionFromRole(
            @Parameter(description = "ID of role") @PathVariable @NonNull Long roleId,
            @Parameter(description = "ID of permission to remove") @PathVariable @NonNull Long permissionId) {
        
        try {
            log.trace("[RoleController] TRACE - removePermissionFromRole() called with roleId=" + roleId + ", permissionId=" + permissionId + ", timestamp=" + System.currentTimeMillis());
            log.debug("[RoleController] DEBUG - removePermissionFromRole() - DELETE /api/roles/" + roleId + "/permissions/" + permissionId + " - Removing permission from role");
            boolean removed = roleService.removePermissionFromRole(roleId, permissionId);
            
            if (removed) {
                log.info("[RoleController] INFO - removePermissionFromRole() - Permission removed from role successfully: roleId=" + roleId + ", permissionId=" + permissionId);
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                log.warn("[RoleController] WARN - removePermissionFromRole() - Role or permission not found for removal: roleId=" + roleId + ", permissionId=" + permissionId);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            log.error("[RoleController] ERROR - removePermissionFromRole() - Failed to remove permission from role: roleId=" + roleId + ", permissionId=" + permissionId, e);
            throw e;
        }
    }
}
