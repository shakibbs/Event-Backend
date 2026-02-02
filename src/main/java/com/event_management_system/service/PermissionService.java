package com.event_management_system.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.dto.PermissionRequestDTO;
import com.event_management_system.dto.PermissionResponseDTO;
import com.event_management_system.entity.Permission;
import com.event_management_system.mapper.PermissionMapper;
import com.event_management_system.repository.PermissionRepository;

import jakarta.annotation.PostConstruct;

@Service
public class PermissionService {

    @Autowired
    private ApplicationLoggerService log;

    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private PermissionMapper permissionMapper;

    @Transactional
    public PermissionResponseDTO createPermission(PermissionRequestDTO permissionRequestDTO) {
        log.trace("[PermissionService] TRACE - createPermission() called with name=" + permissionRequestDTO.getName());
        
        log.debug("[PermissionService] DEBUG - createPermission() - Creating permission entity from DTO");
        Permission permission = permissionMapper.toEntity(permissionRequestDTO);
        permission.recordCreation("system");
        Permission savedPermission = permissionRepository.save(permission);
        
        log.info("[PermissionService] INFO - Permission created successfully: permissionId=" + savedPermission.getId() + ", name=" + savedPermission.getName());
        
        return permissionMapper.toDto(savedPermission);
    }

    @Transactional(readOnly = true)
    public Optional<PermissionResponseDTO> getPermissionById(@NonNull Long id) {
        return permissionRepository.findById(id)
                .map(permissionMapper::toDto);
    }

    @Transactional
    public Optional<PermissionResponseDTO> updatePermission(@NonNull Long id, @NonNull PermissionRequestDTO permissionRequestDTO) {
        log.trace("[PermissionService] TRACE - updatePermission() called with permissionId=" + id + ", name=" + permissionRequestDTO.getName());
        
        return permissionRepository.findById(id).map(existingPermission -> {
            
            log.debug("[PermissionService] DEBUG - updatePermission() - Updating permission entity");
            permissionMapper.updateEntity(permissionRequestDTO, existingPermission);
            existingPermission.recordUpdate("system");
            Permission updatedPermission = permissionRepository.save(existingPermission);
            
            log.info("[PermissionService] INFO - Permission updated successfully: permissionId=" + updatedPermission.getId() + ", name=" + updatedPermission.getName());
            
            return permissionMapper.toDto(updatedPermission);
        });
    }

    @Transactional
    public boolean deletePermission(@NonNull Long id) {
        log.trace("[PermissionService] TRACE - deletePermission() called with permissionId=" + id);
        
        return permissionRepository.findById(id).map(permission -> {
            log.debug("[PermissionService] DEBUG - deletePermission() - Marking permission as deleted");
            String permissionName = permission.getName();
            permission.markDeleted();
            permissionRepository.save(permission);
            
            log.info("[PermissionService] INFO - Permission deleted successfully: permissionId=" + id + ", name=" + permissionName);
            
            return true;
        }).orElse(false);
    }
    
    @Transactional(readOnly = true)
    public List<PermissionResponseDTO> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAllByDeletedFalse();
        return permissions.stream()
                .map(permissionMapper::toDto)
                .collect(Collectors.toList());
    }

    @PostConstruct
    @Transactional
    public void initializeDefaultPermissions() {
        if (permissionRepository.count() > 0) {
            return;
        }

        // SuperAdmin permissions
        createPermissionIfNotExists("user.manage.all", "Can manage all users in the system");
        createPermissionIfNotExists("role.manage.all", "Can manage all roles in the system");
        createPermissionIfNotExists("event.manage.all", "Can manage all events in the system");
        createPermissionIfNotExists("event.approve", "Can approve pending events");
        createPermissionIfNotExists("event.hold", "Can hold/pause events");
        createPermissionIfNotExists("event.reactivate", "Can reactivate held events");
        createPermissionIfNotExists("system.config", "Can configure system settings");
        createPermissionIfNotExists("history.view.all", "Can view all users' history (login, password, activity)");

        // Admin permissions
        createPermissionIfNotExists("user.manage.own", "Can manage own users/team");
        createPermissionIfNotExists("event.manage.own", "Can manage own events");
        createPermissionIfNotExists("event.view.all", "Can view all events");
        createPermissionIfNotExists("event.invite", "Can invite users to events");

        // Attendee permissions
        createPermissionIfNotExists("event.view.public", "Can view public events");
        createPermissionIfNotExists("event.view.invited", "Can view invited events");
        createPermissionIfNotExists("event.attend", "Can attend events");
        
        // Shared permissions (Admin & Attendee)
        createPermissionIfNotExists("history.view.own", "Can view own history (login, password, activity)");
    }

    private void createPermissionIfNotExists(String name, String description) {
        if (!permissionRepository.findByName(name).isPresent()) {
            Permission permission = new Permission();
            permission.setName(name);
            permission.setDescription(description);
            permission.recordCreation("system");
            permissionRepository.save(permission);
        }
    }
}

