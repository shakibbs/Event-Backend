package com.event_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.Permission;
import com.event_management_system.entity.Role;
import com.event_management_system.entity.RolePermission;

/**
 * RolePermissionRepository
 * 
 * Manages database operations for RolePermission entity
 * (composite entity linking roles with permissions)
 * 
 * Purpose:
 * - Map which permissions belong to which roles
 * - Query role-permission relationships
 * - Manage permission assignments to roles
 * 
 * Spring Data JPA automatically implements simple queries!
 * All methods are auto-generated from method names.
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.RolePermissionId> {

    /**
     * Find all permissions for a specific role
     * 
     * @param role The role entity
     * @return List of role-permission mappings for this role
     */
    List<RolePermission> findByRole(Role role);

    /**
     * Find a specific role-permission relationship
     * 
     * @param role The role entity
     * @param permission The permission entity
     * @return Optional containing the role-permission mapping, or empty if not found
     */
    Optional<RolePermission> findByRoleAndPermission(Role role, Permission permission);

    /**
     * Delete a specific role-permission relationship
     * 
     * @param role The role entity
     * @param permission The permission entity
     */
    void deleteByRoleAndPermission(Role role, Permission permission);

    /**
     * Check if a role has a specific permission
     * 
     * @param role The role entity
     * @param permission The permission entity
     * @return true if role has this permission, false otherwise
     */
    boolean existsByRoleAndPermission(Role role, Permission permission);

    /**
     * Find all roles that have a specific permission
     * 
     * @param permission The permission entity
     * @return List of role-permission mappings with this permission
     */
    List<RolePermission> findByPermission(Permission permission);

    /**
     * Find role-permissions by role ID
     * 
     * @param roleId The role ID
     * @return List of role-permission mappings for this role ID
     */
    List<RolePermission> findByIdRoleId(Long roleId);

    /**
     * Find role-permissions by permission ID
     * 
     * @param permissionId The permission ID
     * @return List of role-permission mappings with this permission ID
     */
    List<RolePermission> findByIdPermissionId(Long permissionId);
}
