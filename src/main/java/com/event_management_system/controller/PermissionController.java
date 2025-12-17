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
    private ApplicationLoggerService logger;

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
            logger.traceWithContext("PermissionController", "createPermission() called with name={}, timestamp={}", permissionRequestDTO.getName(), System.currentTimeMillis());
            logger.debugWithContext("PermissionController", "POST /api/permissions - Creating permission: name={}, description={}", permissionRequestDTO.getName(), permissionRequestDTO.getDescription());
            PermissionResponseDTO savedPermission = permissionService.createPermission(permissionRequestDTO);
            logger.infoWithContext("PermissionController", "Permission created successfully: permissionId={}, name={}", savedPermission.getId(), savedPermission.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPermission);
        } catch (Exception e) {
            logger.errorWithContext("PermissionController", "Failed to create permission: name={}", e);
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
            logger.traceWithContext("PermissionController", "updatePermission() called with permissionId={}, name={}", id, permissionDetails.getName());
            logger.debugWithContext("PermissionController", "PUT /api/permissions/{} - Updating permission: name={}", id, permissionDetails.getName());
            Optional<PermissionResponseDTO> permissionOptional = permissionService.updatePermission(id, permissionDetails);
            
            if (permissionOptional.isPresent()) {
                logger.infoWithContext("PermissionController", "Permission updated successfully: permissionId={}, name={}", id, permissionOptional.get().getName());
                return ResponseEntity.ok(permissionOptional.get());
            } else {
                logger.warnWithContext("PermissionController", "Permission not found for update: permissionId={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.errorWithContext("PermissionController", "Failed to update permission: permissionId={}", e);
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
            logger.traceWithContext("PermissionController", "deletePermission() called with permissionId={}, timestamp={}", id, System.currentTimeMillis());
            logger.debugWithContext("PermissionController", "DELETE /api/permissions/{} - Deleting permission", id);
            boolean deleted = permissionService.deletePermission(id);
            
            if (deleted) {
                logger.infoWithContext("PermissionController", "Permission deleted successfully: permissionId={}", id);
                return ResponseEntity.ok().build();
            } else {
                logger.warnWithContext("PermissionController", "Permission not found for deletion: permissionId={}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            logger.errorWithContext("PermissionController", "Failed to delete permission: permissionId={}", e);
            throw e;
        }
    }
}