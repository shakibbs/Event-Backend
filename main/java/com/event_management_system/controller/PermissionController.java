package com.event_management_system.controller;

import java.util.List;
import java.util.Optional;

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

import com.event_management_system.dto.PermissionRequestDTO;
import com.event_management_system.dto.PermissionResponseDTO;
import com.event_management_system.service.ApplicationLoggerService;
import com.event_management_system.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/permissions")
@Tag(name = "Permission Management", description = "APIs for managing permissions in system")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;
    
    @Autowired
    private ApplicationLoggerService log;

    @PostMapping
    @Operation(summary = "Create a new permission", description = "Creates a new permission with provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Permission created successfully",
                    content = @Content(schema = @Schema(implementation = PermissionResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or validation failed"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PermissionResponseDTO> createPermission(
            @Parameter(description = "Permission details to be created", required = true)
            @Valid @RequestBody @NonNull PermissionRequestDTO permissionRequestDTO) {
        
        try {
            log.trace("[PermissionController] TRACE - createPermission() called with name=" + permissionRequestDTO.getName() + ", timestamp=" + System.currentTimeMillis());
            log.debug("[PermissionController] DEBUG - createPermission() - POST /api/permissions - Creating permission: name=" + permissionRequestDTO.getName() + ", description=" + permissionRequestDTO.getDescription());
            PermissionResponseDTO savedPermission = permissionService.createPermission(permissionRequestDTO);
            log.info("[PermissionController] INFO - createPermission() - Permission created successfully: permissionId=" + savedPermission.getId() + ", name=" + savedPermission.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPermission);
        } catch (Exception e) {
            log.error("[PermissionController] ERROR - createPermission() - Failed to create permission: name=" + permissionRequestDTO.getName(), e);
            throw e;
        }
    }

    @GetMapping
    @Operation(summary = "Get all permissions", description = "Retrieves a list of all permissions in system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permissions retrieved successfully",
                    content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<PermissionResponseDTO>> getAllPermissions() {
        List<PermissionResponseDTO> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get permission by ID", description = "Retrieves a specific permission by its unique identifier")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permission found and retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PermissionResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Permission not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PermissionResponseDTO> getPermissionById(
            @Parameter(description = "Unique identifier of permission", required = true, example = "1")
            @PathVariable @NonNull Long id) {
        Optional<PermissionResponseDTO> permission = permissionService.getPermissionById(id);
        if (permission.isPresent()) {
            return ResponseEntity.ok(permission.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing permission", description = "Updates details of an existing permission")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permission updated successfully",
                    content = @Content(schema = @Schema(implementation = PermissionResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or validation failed"),
        @ApiResponse(responseCode = "404", description = "Permission not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<PermissionResponseDTO> updatePermission(
            @Parameter(description = "Unique identifier of permission to update", required = true, example = "1")
            @PathVariable @NonNull Long id,
            @Parameter(description = "Updated permission details", required = true)
            @Valid @RequestBody @NonNull PermissionRequestDTO permissionDetails) {
        
        try {
            log.trace("[PermissionController] TRACE - updatePermission() called with permissionId=" + id + ", name=" + permissionDetails.getName());
            log.debug("[PermissionController] DEBUG - updatePermission() - PUT /api/permissions/" + id + " - Updating permission: name=" + permissionDetails.getName());
            Optional<PermissionResponseDTO> permissionOptional = permissionService.updatePermission(id, permissionDetails);
            
            if (permissionOptional.isPresent()) {
                log.info("[PermissionController] INFO - updatePermission() - Permission updated successfully: permissionId=" + id + ", name=" + permissionOptional.get().getName());
                return ResponseEntity.ok(permissionOptional.get());
            } else {
                log.warn("[PermissionController] WARN - updatePermission() - Permission not found for update: permissionId=" + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("[PermissionController] ERROR - updatePermission() - Failed to update permission: permissionId=" + id, e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a permission", description = "Soft-deletes a permission by marking it as deleted")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Permission deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Permission not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deletePermission(
            @Parameter(description = "Unique identifier of permission to delete", required = true, example = "1")
            @PathVariable @NonNull Long id) {
        
        try {
            log.trace("[PermissionController] TRACE - deletePermission() called with permissionId=" + id + ", timestamp=" + System.currentTimeMillis());
            log.debug("[PermissionController] DEBUG - deletePermission() - DELETE /api/permissions/" + id + " - Deleting permission");
            boolean deleted = permissionService.deletePermission(id);
            
            if (deleted) {
                log.info("[PermissionController] INFO - deletePermission() - Permission deleted successfully: permissionId=" + id);
                return ResponseEntity.ok().build();
            } else {
                log.warn("[PermissionController] WARN - deletePermission() - Permission not found for deletion: permissionId=" + id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            log.error("[PermissionController] ERROR - deletePermission() - Failed to delete permission: permissionId=" + id, e);
            throw e;
        }
    }
}
