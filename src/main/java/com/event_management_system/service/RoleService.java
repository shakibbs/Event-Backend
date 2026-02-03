package com.event_management_system.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
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
@DependsOn("permissionService")
public class RoleService {

    @Autowired
    private ApplicationLoggerService log;

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
        log.trace("[RoleService] TRACE - createRole() called with name=" + roleRequestDTO.getName());
        
        log.debug("[RoleService] DEBUG - createRole() - Creating role entity from DTO");
        Role role = roleMapper.toEntity(roleRequestDTO);
        role.recordCreation("system");
        Role savedRole = roleRepository.save(role);
        
        if (roleRequestDTO.getPermissionIds() != null && !roleRequestDTO.getPermissionIds().isEmpty()) {
            log.debug("[RoleService] DEBUG - createRole() - Assigning " + roleRequestDTO.getPermissionIds().size() + " permissions to role");
            for (Long permissionId : roleRequestDTO.getPermissionIds()) {
                if (permissionId != null) {
                    permissionRepository.findById(permissionId).ifPresent(permission -> {
                        RolePermission rolePermission = new RolePermission(savedRole, permission);
                        rolePermissionRepository.save(rolePermission);
                        log.debug("[RoleService] DEBUG - createRole() - Assigned permission: " + permission.getName());
                    });
                }
            }
        }
        
        log.info("[RoleService] INFO - Role created successfully: roleId=" + savedRole.getId() + ", name=" + savedRole.getName());
        
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

    @Transactional(readOnly = true)
    public Set<Permission> getPermissionsForRole(@NonNull Long roleId) {
        return roleRepository.findById(roleId)
                .map(role -> {
                    Set<Permission> permissions = new HashSet<>();
                    if (role.getRolePermissions() != null) {
                        for (RolePermission rp : role.getRolePermissions()) {
                            if (rp.getPermission() != null) {
                                permissions.add(rp.getPermission());
                            }
                        }
                    }
                    return permissions;
                })
                .orElse(new HashSet<>());
    }

    @Transactional
    public Optional<RoleResponseDTO> updateRole(@NonNull Long id, @NonNull RoleRequestDTO roleRequestDTO) {
        log.trace("[RoleService] TRACE - updateRole() called with roleId=" + id + ", name=" + roleRequestDTO.getName());
        
        return roleRepository.findById(id).map(existingRole -> {
            log.debug("[RoleService] DEBUG - updateRole() - Updating role entity");
            roleMapper.updateEntity(roleRequestDTO, existingRole);
            existingRole.recordUpdate("system");
            
            if (roleRequestDTO.getPermissionIds() != null) {
                log.debug("[RoleService] DEBUG - updateRole() - Removing existing permissions for roleId=" + id);
                rolePermissionRepository.findByRole(existingRole).forEach(rolePermissionRepository::delete);
                
                log.debug("[RoleService] DEBUG - updateRole() - Assigning " + roleRequestDTO.getPermissionIds().size() + " new permissions");
                for (Long permissionId : roleRequestDTO.getPermissionIds()) {
                    if (permissionId != null) {
                        permissionRepository.findById(permissionId).ifPresent(permission -> {
                            RolePermission rolePermission = new RolePermission(existingRole, permission);
                            rolePermissionRepository.save(rolePermission);
                            log.debug("[RoleService] DEBUG - updateRole() - Assigned permission: " + permission.getName());
                        });
                    }
                }
            }
            
            Role updatedRole = roleRepository.save(existingRole);
            
            log.info("[RoleService] INFO - Role updated successfully: roleId=" + updatedRole.getId() + ", name=" + updatedRole.getName());
            
            return roleMapper.toDto(updatedRole);
        });
    }

    @Transactional
    public boolean deleteRole(@NonNull Long id) {
        log.trace("[RoleService] TRACE - deleteRole() called with roleId=" + id);
        
        return roleRepository.findById(id).map(role -> {
            log.debug("[RoleService] DEBUG - deleteRole() - Marking role as deleted");
            String roleName = role.getName();
            role.markDeleted();
            roleRepository.save(role);
            
            log.info("[RoleService] INFO - Role deleted successfully: roleId=" + id + ", name=" + roleName);
            
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
        log.debug("[RoleService] DEBUG - removePermissionFromRole() called with roleId={} and permissionId={}", roleId, permissionId);
        Optional<Role> roleOpt = roleRepository.findById(roleId);
        Optional<Permission> permOpt = permissionRepository.findById(permissionId);
        if (roleOpt.isPresent() && permOpt.isPresent()) {
            Role role = roleOpt.get();
            Permission permission = permOpt.get();
            RolePermission.RolePermissionId id = new RolePermission.RolePermissionId(roleId, permissionId);
            RolePermission toRemove = null;
            for (RolePermission rp : role.getRolePermissions()) {
                if (rp.getId().equals(id)) {
                    toRemove = rp;
                    break;
                }
            }
            if (toRemove != null) {
                role.getRolePermissions().remove(toRemove);
                rolePermissionRepository.deleteById(id);
                log.info("[RoleService] INFO - RolePermission deleted for roleId={}, permissionId={}", roleId, permissionId);
                return true;
            } else {
                log.warn("[RoleService] WARN - No RolePermission found in role entity for roleId={}, permissionId={}", roleId, permissionId);
                return false;
            }
        } else {
            log.warn("[RoleService] WARN - Role or Permission not found for roleId={}, permissionId={}", roleId, permissionId);
            return false;
        }
    }
    
    @Transactional
    public boolean addPermissionToRole(@NonNull Long roleId, @NonNull Long permissionId) {
        return assignPermissionToRole(roleId, permissionId);
    }

    @PostConstruct
    @Transactional
    public void initializeDefaultRoles() {
        // Temporarily force recreation - remove this check after first deployment
        // if (roleRepository.count() > 0) {
        //     return;
        // }

        log.info("[RoleService] INFO - Initializing default roles and permissions");
        
        createRoleWithPermissions("SuperAdmin", 
                "user.manage.all", "user.view.all", "role.manage.all", "role.view.all", "event.manage.all", "event.approve", 
                "event.hold", "event.reactivate", "system.config", "history.view.all");

        createRoleWithPermissions("Admin", 
                "user.manage.own", "event.manage.own", "event.view.all", "event.invite", 
                "event.approve", "history.view.own");

        createRoleWithPermissions("Attendee", 
                "event.view.public", "event.view.invited", "event.attend", "history.view.own");
        
        log.info("[RoleService] INFO - Default roles initialization completed");
    }
    
    private void createRoleWithPermissions(String roleName, String... permissionNames) {
        log.debug("[RoleService] DEBUG - Creating/updating role: {}", roleName);
        
        Optional<Role> roleOpt = roleRepository.findByName(roleName);
        final Role role;
        
        if (!roleOpt.isPresent()) {
            Role newRole = new Role();
            newRole.setName(roleName);
            newRole.recordCreation("system");
            role = roleRepository.save(newRole);
            log.info("[RoleService] INFO - Created new role: {} with id={}", roleName, role.getId());
        } else {
            role = roleOpt.get();
            log.debug("[RoleService] DEBUG - Role already exists: {} with id={}", roleName, role.getId());
        }
        
        int assignedCount = 0;
        int skippedCount = 0;
        int notFoundCount = 0;
        
        for (String permissionName : permissionNames) {
            Optional<Permission> permOpt = permissionRepository.findByName(permissionName);
            if (permOpt.isPresent()) {
                Permission permission = permOpt.get();
                boolean exists = rolePermissionRepository.findByRoleIdAndPermissionId(role.getId(), permission.getId()).isPresent();
                if (!exists) {
                    RolePermission rolePermission = new RolePermission(role, permission);
                    rolePermissionRepository.save(rolePermission);
                    assignedCount++;
                    log.debug("[RoleService] DEBUG - Assigned permission '{}' to role '{}'", permissionName, roleName);
                } else {
                    skippedCount++;
                }
            } else {
                notFoundCount++;
                log.warn("[RoleService] WARN - Permission '{}' not found for role '{}'", permissionName, roleName);
            }
        }
        
        log.info("[RoleService] INFO - Role '{}' permissions: {} assigned, {} skipped (already exist), {} not found", 
                roleName, assignedCount, skippedCount, notFoundCount);
    }
}

