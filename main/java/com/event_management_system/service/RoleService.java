package com.event_management_system.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.event_management_system.dto.RoleRequestDTO;
import com.event_management_system.dto.RoleResponseDTO;
import com.event_management_system.entity.Permission;
import com.event_management_system.entity.Role;
import com.event_management_system.entity.RolePermission;
import com.event_management_system.mapper.RoleMapper;
import com.event_management_system.repository.PermissionRepository;
import com.event_management_system.repository.RolePermissionRepository;
import com.event_management_system.repository.RoleRepository;

import jakarta.annotation.PostConstruct;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    
    @Autowired
    private RoleMapper roleMapper;

    @Transactional
    public RoleResponseDTO createRole(RoleRequestDTO roleRequestDTO) {
        Role role = roleMapper.toEntity(roleRequestDTO);
        role.recordCreation("system");
        Role savedRole = roleRepository.save(role);
        
        // Assign permissions if provided
        if (roleRequestDTO.getPermissionIds() != null && !roleRequestDTO.getPermissionIds().isEmpty()) {
            for (Long permissionId : roleRequestDTO.getPermissionIds()) {
                if (permissionId != null) {
                    permissionRepository.findById(permissionId).ifPresent(permission -> {
                        RolePermission rolePermission = new RolePermission(savedRole, permission);
                        rolePermissionRepository.save(rolePermission);
                    });
                }
            }
        }
        
        return roleMapper.toDto(savedRole);
    }

    @Transactional(readOnly = true)
    public Optional<RoleResponseDTO> getRoleById(@NonNull Long id) {
        return roleRepository.findById(id)
                .map(roleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Optional<RoleResponseDTO> getRoleByName(@NonNull String name) {
        return roleRepository.findByName(name)
                .map(roleMapper::toDto);
    }

    @Transactional(readOnly = true)
    public List<RoleResponseDTO> getAllRoles() {
        List<Role> roles = roleRepository.findAllByDeletedFalse();
        return roles.stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<RoleResponseDTO> updateRole(@NonNull Long id, @NonNull RoleRequestDTO roleRequestDTO) {
        return roleRepository.findById(id).map(existingRole -> {
            roleMapper.updateEntity(roleRequestDTO, existingRole);
            existingRole.recordUpdate("system");
            
            // Update permissions if provided
            if (roleRequestDTO.getPermissionIds() != null) {
                // Remove all existing permissions
                rolePermissionRepository.findByRole(existingRole).forEach(rolePermissionRepository::delete);
                
                // Add new permissions
                for (Long permissionId : roleRequestDTO.getPermissionIds()) {
                    if (permissionId != null) {
                        permissionRepository.findById(permissionId).ifPresent(permission -> {
                            RolePermission rolePermission = new RolePermission(existingRole, permission);
                            rolePermissionRepository.save(rolePermission);
                        });
                    }
                }
            }
            
            Role updatedRole = roleRepository.save(existingRole);
            return roleMapper.toDto(updatedRole);
        });
    }

    @Transactional
    public boolean deleteRole(@NonNull Long id) {
        return roleRepository.findById(id).map(role -> {
            role.markDeleted();
            roleRepository.save(role);
            return true;
        }).orElse(false);
    }
    
    @Transactional
    public boolean assignPermissionToRole(@NonNull Long roleId, @NonNull Long permissionId) {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
        
        if (roleOpt.isPresent() && permissionOpt.isPresent()) {
            Role role = roleOpt.get();
            Permission permission = permissionOpt.get();
            
            // Check if this permission is not already assigned
            if (!rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
                RolePermission rolePermission = new RolePermission(role, permission);
                rolePermissionRepository.save(rolePermission);
                return true;
            }
        }
        return false;
    }
    
    @Transactional
    public boolean removePermissionFromRole(@NonNull Long roleId, @NonNull Long permissionId) {
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        Optional<Permission> permissionOpt = permissionRepository.findById(permissionId);
        
        if (roleOpt.isPresent() && permissionOpt.isPresent()) {
            Role role = roleOpt.get();
            Permission permission = permissionOpt.get();
            
            rolePermissionRepository.deleteByRoleAndPermission(role, permission);
            return true;
        }
        return false;
    }
    
    // Alias methods to match RoleController expectations
    @Transactional
    public boolean addPermissionToRole(@NonNull Long roleId, @NonNull Long permissionId) {
        return assignPermissionToRole(roleId, permissionId);
    }

    // Initialize default roles for the system
    @PostConstruct
    @Transactional
    public void initializeDefaultRoles() {
        // Check if roles already exist
        if (roleRepository.count() > 0) {
            return;
        }

        // Create SuperAdmin role with all permissions
        createRoleWithPermissions("SuperAdmin", 
                "user.manage.all", "role.manage.all", "event.manage.all", "system.config", "history.view.all");

        // Create Admin role with limited permissions
        createRoleWithPermissions("Admin", 
                "user.manage.own", "event.manage.own", "event.view.all", "event.invite", "history.view.own");

        // Create Attendee role with basic permissions
        createRoleWithPermissions("Attendee", 
                "event.view.public", "event.view.invited", "event.attend", "history.view.own");
    }
    
    private void createRoleWithPermissions(String roleName, String... permissionNames) {
        if (!roleRepository.findByName(roleName).isPresent()) {
            Role role = new Role();
            role.setName(roleName);
            role.recordCreation("system");
            Role savedRole = roleRepository.save(role);
            
            // Assign permissions through RolePermission junction table
            for (String permissionName : permissionNames) {
                permissionRepository.findByName(permissionName).ifPresent(permission -> {
                    RolePermission rolePermission = new RolePermission(savedRole, permission);
                    rolePermissionRepository.save(rolePermission);
                });
            }
        }
    }
}