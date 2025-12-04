package com.event_management_system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.event_management_system.entity.Permission;
import com.event_management_system.entity.Role;
import com.event_management_system.entity.RolePermission;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermission.RolePermissionId> {

    /**
     * Find all permissions for a specific role
     */
    List<RolePermission> findByRole(Role role);

    /**
     * Find a specific role-permission relationship
     */
    Optional<RolePermission> findByRoleAndPermission(Role role, Permission permission);

    /**
     * Delete a specific role-permission relationship
     */
    void deleteByRoleAndPermission(Role role, Permission permission);

    /**
     * Check if a role has a specific permission
     */
    boolean existsByRoleAndPermission(Role role, Permission permission);

    /**
     * Find all roles that have a specific permission
     */
    List<RolePermission> findByPermission(Permission permission);

    /**
     * Find by role ID
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.id.roleId = :roleId")
    List<RolePermission> findByRoleId(@Param("roleId") Long roleId);

    /**
     * Find by permission ID
     */
    @Query("SELECT rp FROM RolePermission rp WHERE rp.id.permissionId = :permissionId")
    List<RolePermission> findByPermissionId(@Param("permissionId") Long permissionId);
}
